package grpc_service

import com.example.api.exception.EmployeeNotFoundException
import com.example.api.repository.model.UTxO
import com.google.protobuf.Empty
import cs236351.txservice.*
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import multipaxos.BroadcastServiceImpl
import org.springframework.http.HttpStatus
import zk_service.ZkRepository
import java.net.InetAddress
import java.util.*
import kotlin.collections.ArrayList

object TxClient {
    @JvmStatic
    fun main(args: Array<String>) {
        val channel = ManagedChannelBuilder.forAddress("localhost", 8090)
            .usePlaintext()
            .build()
        val stub: TxServiceGrpc.TxServiceBlockingStub = TxServiceGrpc.newBlockingStub(channel)
        val txId : TxId = TxId.newBuilder().setId(1).build()
        val tx : Transaction = Transaction.newBuilder()
            .setTxId(txId)
            .addInputs(Utxo.newBuilder().setTxId(txId).setAddress("1"))
            .addOutputs(Transfer.newBuilder().setAddress("2").setAmount(3))
            .build()
        stub.insertTx(tx)
        val response : TransactionList = stub.getAllTx(Request.newBuilder().build())
        println(response)
        channel.shutdown()
    }

    lateinit var stub: TxServiceGrpc.TxServiceBlockingStub
    //lateinit var localStub: TxServiceGrpc.TxServiceBlockingStub
    var channelStack: Stack<ManagedChannel> = Stack()
    lateinit var channel: ManagedChannel
    private val shardRepository: ShardsRepository = ShardsRepository

    init {
        val address = InetAddress.getLocalHost()
        val ip = address.hostAddress
        /*val channel = ManagedChannelBuilder.forAddress(ip, 8090)
            .usePlaintext()
            .build()
        this.localStub = TxServiceGrpc.newBlockingStub(channel)*/
        //this.channel.shutdown()
    }

    private fun connectStub(address: Address)  {
        val ip = shardRepository.getShardLeaderIp(address)
        val channel =  ManagedChannelBuilder.forAddress(ip, 8090)
            .usePlaintext()
            .build()
        this.stub = TxServiceGrpc.newBlockingStub(channel)
        channelStack.push(channel)
    }

    private fun disconnectStub() {
        if (channelStack.isNotEmpty())
            channelStack.pop().shutdown()
    }

    fun empty() : Empty {
        return Empty.newBuilder().build()
    }

    private fun request(address: Address) : Request {
        return Request.newBuilder().setAddress(address).build()
    }

    fun trRequest(source: Address, txId: Id, tr: com.example.api.repository.model.Transfer) : TrRequest {
        return TrRequest.newBuilder().setSource(source).setTxId(txid(txId)).setTr(transfer(tr.address, tr.amount)).build()
    }

    fun trRequest(source: Address, txId: Id, tr: Transfer) : TrRequest {
        return TrRequest.newBuilder().setSource(source).setTxId(txid(txId)).setTr(tr).build()
    }

    private fun txid(id: Id) : TxId {
        return TxId.newBuilder().setId(id).build()
    }

    fun utxo(id: Id, address: Address, value: Value) : Utxo {
        val txId : TxId = TxId.newBuilder().setId(id).build()
        return Utxo.newBuilder().setTxId(txId).setAddress(address).setValue(value).build()
    }

    private fun transfer(address: Address, amount: Value) : Transfer {
        return Transfer.newBuilder().setAddress(address).setAmount(amount).build()
    }

    private fun transaction(id: Id, inputs: List<Utxo>, outputs: List<Transfer>) : Transaction {
        val txId : TxId = TxId.newBuilder().setId(id).build()
        return Transaction.newBuilder().setTxId(txId).addAllInputs(inputs).addAllOutputs(outputs).build()
    }

    fun ledgerTxEntry(timestamp: TimeStamp, tx: Transaction) : LedgerTxEntry {
        return LedgerTxEntry.newBuilder().setTimestamp(timestamp).setTx(tx).build()
    }

    private fun fromClientUtxo(utxo: Utxo) : UTxO {
        return UTxO(utxo.txId.id, utxo.address, utxo.value)
    }

    fun toClientTransaction(tx: com.example.api.repository.model.Transaction) : Transaction {
        val inputs : List<Utxo> = tx.inputs.map { utxo(it.txId, it.address, it.value) }
        val outputs: List<Transfer> = tx.outputs.map { transfer(it.address, it.amount) }
        return this.transaction(tx.id, inputs, outputs)
    }

    fun fromClientTransaction(tx: Transaction) : com.example.api.repository.model.Transaction {
        val inputs : List<UTxO> = tx.inputsList.map { UTxO(it.txId.id, it.address, it.value) }
        val outputs: List<com.example.api.repository.model.Transfer> = tx.outputsList.map { com.example.api.repository.model.Transfer(inputs[0].address, it.address, it.amount) }
        return com.example.api.repository.model.Transaction(tx.txId.id, inputs, outputs)
    }

    private fun toClientTransactionList(txList: List<com.example.api.repository.model.Transaction>) : TransactionList {
        return TransactionList.newBuilder().addAllTxList(txList.map { toClientTransaction(it) }).build()
    }

    fun insertTx(tx: com.example.api.repository.model.Transaction): String {
        var res = Ack.NO
        try {
            tx.id = ZkRepository.getTimestamp()
            connectStub(tx.inputs[0].address)
            res = stub.insertTx(toClientTransaction(tx)).ack
        } catch (e: Throwable) {
            println("### $e ###")
        } finally {
            disconnectStub()
        }
        return if (res == Ack.YES) "success" else "fail"
    }

    private fun deleteTx(txId: Id) {

    }

    private fun existsTx(txId: Id): Boolean {
        return this.stub.existsTx(txid(txId)).exists
    }

    private fun getTx(txId: Id): com.example.api.repository.model.Transaction {
        return fromClientTransaction(this.stub.getTx(txid(txId)))
    }

    fun sendTr(txId: Id, source: Address, tr: Transfer): String {
        var res = Ack.NO
        val request = trRequest(source, ZkRepository.getTimestamp(), tr)
        //ZkRepository.logTransfer(request)
        try {
            connectStub(tr.address)
            res = stub.sendTr(request).ack
        } catch (e: Throwable) {
        println("### $e ###")
        } finally {
            disconnectStub()
        }
        return if (res == Ack.YES) "success" else "fail"
    }

    fun addUtxo(txId: Id, source: Address, tr: Transfer) {
        val request = trRequest(source, txId, tr)
        ZkRepository.logTransfer(request)
        try {
            connectStub(request.tr.address)
            stub.addUtxo(request)
        } catch (e: Throwable) {
            println("### $e ###")
        } finally {
            disconnectStub()
        }
    }

    fun removeUtxo(trRequest: TrRequest) {
        try {
            connectStub(trRequest.source)
            stub.removeUtxo(trRequest)
        } catch (e: Throwable) {
            println("### $e ###")
        } finally {
            disconnectStub()
        }
    }

    fun commitTr(trRequest: TrRequest) {
        try {
            connectStub(trRequest.source)
            stub.commitTr(trRequest)
        } catch (e: Throwable) {
            println("### $e ###")
        } finally {
            disconnectStub()
        }
    }

    private fun getAllUtxo(address: Address) : List<UTxO> {
        var utxoList : ArrayList<UTxO> = ArrayList()
        try {
            connectStub(address)
            utxoList = ArrayList(stub.getAllUtxo(request(address)).utxoListList.map { fromClientUtxo(it) })
        } catch (e: Throwable) {
            println("### $e ###")
        } finally {
            disconnectStub()
        }
        return utxoList
    }

    private fun getAllTx(address: Address, limit: Int?): List<com.example.api.repository.model.Transaction> {
        var txList : ArrayList<com.example.api.repository.model.Transaction> = ArrayList()
        try {
            connectStub(address)
            txList = ArrayList(stub.getAllTx(request(address)).txListList.map { fromClientTransaction(it) })
        } catch (e: Throwable) {
            println("### $e ###")
        } finally {
            disconnectStub()
        }
        txList.sortBy { it.id }
        if (limit != null && limit >= 0) {
            return txList.takeLast(limit)
        }
        return txList
    }

    private fun getLedger(limit: Int?) : List<com.example.api.repository.model.Transaction> {
        val ledger = ArrayList<LedgerTxEntry>()
        try {
            for (ips in shardRepository.ips.values) {
                println(ips[0])
                connectStub(ips[0])
                ledger += ArrayList(stub.getLedger(null).txListList)
                disconnectStub()
            }
        } catch (e: Throwable) {
            println("### $e ###")
        } finally {
            disconnectStub()
        }
        /*println("###################")
        for (i in ledger) {
            println(i.tx.txId.id)
        }*/
        ledger.sortBy { it.timestamp }
        val res = ledger.map { fromClientTransaction(it.tx) }
        if (limit != null && limit >= 0) {
            return res.takeLast(limit)
        }
        return res
    }

    /** ###################################### API FUNCTIONS ###################################### **/

    fun getAllUnspentTxOutput(address: Address): List<UTxO> = this.getAllUtxo(address)

    fun getAllTransactions(address: Address, limit: Int?): List<com.example.api.repository.model.Transaction> = this.getAllTx(address, limit)

    fun getTransactionById(id: Long): com.example.api.repository.model.Transaction = this.getTx(id)

    fun getTransactionLedger(limit: Int?): List<com.example.api.repository.model.Transaction> = this.getLedger(limit)

    fun submitTransaction(tx: com.example.api.repository.model.Transaction) = this.insertTx(tx)
        /*try {
            println("sending prop")
            BroadcastServiceImpl.send(tx.toString())
            connectStub(tx.inputs[0].address)
            stub.insertTx(toClientTransaction(tx))
        } catch (e: Throwable) {
            println("### $e ###")
        } finally {
            disconnectStub()
        }
    }*/

    fun submitTransactionList(txList: List<com.example.api.repository.model.Transaction>) {
        try {
            connectStub(txList[0].inputs[0].address)
            stub.insertTxList(toClientTransactionList(txList))
        } catch (e: Throwable) {
            println("### $e ###")
        } finally {
            disconnectStub()
        }
    }

    fun submitTransfer(transfer: com.example.api.repository.model.Transfer) {
        this.sendTr(-1, transfer.source, transfer(transfer.address, transfer.amount))
    }
}
