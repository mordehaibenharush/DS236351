package multipaxos

import com.example.api.repository.model.Transfer
import com.example.api.repository.model.UTxO
import com.google.gson.Gson
import com.google.protobuf.ByteString
import com.google.protobuf.Empty
import com.google.protobuf.kotlin.toByteStringUtf8
import cs236351.broadcast.BroadcastServiceGrpc
import cs236351.broadcast.Msg
import cs236351.broadcast.Transaction
import cs236351.txservice.TrRequest
import cs236351.txservice.Utxo
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.*
import grpc_service.ShardsRepository.getIpFromId
import grpc_service.ShardsRepository.getShardIds
import grpc_service.ShardsRepository.getIp
import grpc_service.ShardsRepository.getShardLeaderId
import grpc_service.ShardsRepository.getShardLeaderIpFromId
import grpc_service.TransactionRepository
import grpc_service.TxClient
import java.util.*

enum class msgType {INSERT_TRANSACTION, INSERT_UTXO, DELETE_UTXO, SPEND_UTXO, ROLLBACK_UTXO}

fun main(args: Array<String>) {
    val tx = com.example.api.repository.model.Transaction(1,
        listOf(UTxO(1,"0.0.0.0",5), UTxO(2,"0.0.0.1",6)),
        listOf(Transfer("0.0.0.0", "0.0.0.1", 3)))
    //val str = BroadcastServiceImpl.transactionToMsg(tx)
    //println(str)
    //val tt = BroadcastServiceImpl.msgToTransaction(str)

    /*runBlocking {
        val id = args[0].toInt()
        val service = BroadcastServiceImpl
        launch { service.start(id) }
        *//*val server: Server = ServerBuilder
        .forPort(8090 + id)
        .addService(service).build()
    server.start()*//*
        println("$id - started server")
        withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
            System.`in`.read()
        }
        println("$id - start sending messages")
        service.startGeneratingMessages(id)
    }*/
    /*withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
        server.awaitTermination()
    }*/
}

object BroadcastServiceImpl : BroadcastServiceGrpc.BroadcastServiceImplBase() {
    lateinit var proposer: Proposer
    lateinit var atomicBroadcast: AtomicBroadcast<String>
    lateinit var chans:  Map<ID, ManagedChannel>
    lateinit var stub: BroadcastServiceGrpc.BroadcastServiceBlockingStub
    var channelStack: Stack<ManagedChannel> = Stack()

    val omega = object : OmegaFailureDetector<ID> {
        override val leader: ID get() = getShardLeaderId(getIp())
        override fun addWatcher(observer: suspend () -> Unit) { ZkRepository.setWatcher(observer) }
    }

    val biSerializer = object : ByteStringBiSerializer<String> {
        override fun serialize(obj: String) = obj.toByteStringUtf8()
        override fun deserialize(serialization: ByteString) = serialization.toStringUtf8()!!
    }

    suspend fun start(i: Int) = coroutineScope {
            // Displays all debug messages from gRPC
            // org.apache.log4j.BasicConfigurator.configure()

            // Take the ID as the port number
            val id = getIp().split('.').last().toInt()

            // Init services
            val learnerService = LearnerService(this)
            val acceptorService = AcceptorService(id)

            // Build gRPC server
            val server = ServerBuilder.forPort(9010)
                .apply {
                    //if (id != omega.leader) // Apply your own logic: who should be an acceptor
                        addService(acceptorService)
                }
                .apply { // Apply your own logic: who should be a learner
                        addService(learnerService)
                }
                .apply {
                        addService(this@BroadcastServiceImpl)
                }
                .build()

            atomicBroadcast = object : AtomicBroadcast<String>(learnerService, biSerializer) {
                override suspend fun _send(byteString: ByteString) {
                    connectStub()
                    stub.suggest(Msg.newBuilder().setMsg(byteString.toStringUtf8()).build())
                    disconnectStub()
                }
                override fun _deliver(byteString: ByteString) = listOf(biSerializer(byteString))
            }
            //println("$id - about to start internal server")
            withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
                server.start()
            }
            //println("$id - internal server started")
            // Create channels with clients
            chans = getShardIds(id)/*listOf(0,1,3)*/!!.associateWith {
                ManagedChannelBuilder.forAddress(getIpFromId(it)/*"localhost"*/, 9010).usePlaintext().build()!!
            }

            /*
             * Don't forget to add the list of learners to the learner service.
             * The learner service is a reliable broadcast service and needs to
             * have a connection with all processes that participate as learners
             */
            learnerService.learnerChannels = chans.filterKeys { getIpFromId(it) != getIp() /*it != id*/ }.values.toList()

            // Create a proposer, not that the proposers id's id and
            // the acceptors id's must be all unique (they break symmetry)
            proposer = Proposer(
                id = id, omegaFD = omega, scope = this, acceptors = chans,
                thisLearner = learnerService,
            )
            //println("$id - about to start proposer")
            // Starts The proposer
            proposer.start()

            println("$id - started broadcast service")
            withContext(Dispatchers.IO) { startRecievingMessages(atomicBroadcast) }

            withContext(Dispatchers.IO) { // Operations that block the current thread should be in a IO context
                server.awaitTermination()
            }
    }

    private fun connectStub()  {
        val ip = getShardLeaderIpFromId(omega.leader)
        val channel =  ManagedChannelBuilder.forAddress(ip, 9010)
            .usePlaintext()
            .build()
        this.stub = BroadcastServiceGrpc.newBlockingStub(channel)
        channelStack.push(channel)
    }

    private fun disconnectStub() {
        if (channelStack.isNotEmpty())
            channelStack.pop().shutdown()
    }

    private fun CoroutineScope.startRecievingMessages(atomicBroadcast: AtomicBroadcast<String>) {
        launch {
            for ((`seq#`, msg) in atomicBroadcast.stream) {
                println("Message #$`seq#`: ${msgType.values()[msg.split('|')[1].toInt()]}  received!")
                msgDispatch(msg)
            }
        }
    }

    fun send(type: msgType, prop: String) {
        val msg = getIp() + "|" + type.ordinal.toString() + "|" + prop
        CoroutineScope(Dispatchers.IO).launch {
                println("Adding Proposal - $type")
                atomicBroadcast.send(msg)
        }
    }

    fun startGeneratingMessages(
        id: Int,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            println("Started Generating Messages")
            (1..100).forEach {
                delay(1000)
                val prop = "[Value no $it from $id]"
                    .also { println("Adding Proposal $it") }
                atomicBroadcast.send(prop)
            }
        }
    }

    fun transactionToMsg(tx: cs236351.txservice.Transaction) : String {
        return Gson().toJson(tx)
    }

    fun msgToTransaction(msg: String) : cs236351.txservice.Transaction {
        return Gson().fromJson(msg, cs236351.txservice.Transaction::class.java)
    }

    fun transferToMsg(tr: TrRequest) : String {
        return Gson().toJson(tr)
    }

    fun msgToTransfer(msg: String) : TrRequest {
        return Gson().fromJson(msg, TrRequest::class.java)
    }

    fun utxoToMsg(utxo: Utxo) : String {
        return Gson().toJson(utxo)
    }

    fun msgToUtxo(msg: String) : Utxo {
        return Gson().fromJson(msg, Utxo::class.java)
    }

    fun msgDispatch(msg: String) {
        val msgContents = msg.split('|')
        val proposer = msgContents[0]
        val type = msgType.values()[msgContents[1].toInt()]
        val body = msgContents[2]
        when(type) {
            msgType.INSERT_TRANSACTION ->
            {
                TransactionRepository.insertTx(msgToTransaction(body))
            }
            msgType.INSERT_UTXO ->
            {
                val trRequest = msgToTransfer(body)
                if (proposer == getIp())
                    TxClient.commitTr(trRequest)
                TransactionRepository.insertUtxo(trRequest.txId.id, trRequest.tr.address, trRequest.tr.amount)
            }
            msgType.DELETE_UTXO ->
            {
                val trRequest = msgToTransfer(body)
                if (proposer == getIp())
                    TxClient.commitTr(trRequest)
                TransactionRepository.removeUtxoByValue(trRequest.txId.id, trRequest.source, trRequest.tr.amount)
            }
            msgType.SPEND_UTXO ->
            {
                val utxo = msgToUtxo(body)
                TransactionRepository.spendUtxo(utxo.address, utxo.txId.id, utxo.value)
            }
            msgType.ROLLBACK_UTXO ->
            {
                val utxo = msgToUtxo(body)
                TransactionRepository.addUtxo(utxo.txId.id, utxo.address, utxo.value)
            }
        }
    }

    override fun send(request: Transaction, responseObserver: StreamObserver<Empty>) {
        runBlocking { proposer.addProposal(request.toByteString()) }
        responseObserver.onNext(Empty.newBuilder().build())
        responseObserver.onCompleted()
    }

    override fun suggest(request: Msg, responseObserver: StreamObserver<Empty>) {
        runBlocking { proposer.addProposal(request.msg.toByteStringUtf8()) }
        responseObserver.onNext(Empty.newBuilder().build())
        responseObserver.onCompleted()
    }
}
