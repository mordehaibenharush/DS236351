package zk_service

import com.example.api.repository.model.Transfer
import cs236351.txservice.TrRequest
import cs236351.txservice.TxId
import cs236351.txservice.trRequest
import grpc_service.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.InetAddress

typealias TimeStamp = Long
//typealias GetTimestamp = suspend CoroutineScope.(Array<String>, client: ZooKeeperKt) -> TimeStamp

fun main(args: Array<String>) {
    val zkr = ZkRepository
    val m = zkr.initMutex()
    CoroutineScope(Dispatchers.IO).launch {
        launch {
            m.lock()
            println("${args[0]} got the lock!!!")
            delay(20_000)
            println("${args[0]} releasing lock...")
            m.unlock()
        }
    }
    /*zkr.logTransferEntry(
         "/log/0.0.0.0_1_0.0.0.1_999"
    )
    zkr.logTransferEntry(
        "/log/0.0.0.2_3_0.0.0.3_222"
    )
    CoroutineScope(Dispatchers.IO).launch {
        zkr.queryLog()
    }
    runBlocking { delay(10_000) }
    zkr.commitTransferEntry("/log/0.0.0.0_1_0.0.0.1_999")*/
}

object ZkRepository {
    lateinit var zk : ZookeeperKtClient
    lateinit var zkConnection: ZkConnection
    lateinit var membership: Membership
    lateinit var queryMembershipJob: Job
    lateinit var chan: Channel<ZChildren>
    lateinit var txMutex: ZKMutex
    //var utxoMutexMap = hashMapOf<Address, ZKMutex>()
    //private val shardsPath : Array<String> = arrayOf("1",)
    private val globalClockPath : String = "/clock/"
    private val logPath : String = "/log"

    /*fun updateLeader(shard: Shard, address: Address) {
        if (!(zk.existsZNodeData(leadersIpPath + shard.toString()))) {
            zk.createZNodeData(globalClockPath, address.toByteArray(), CreateMode.EPHEMERAL)
        }
    }

    fun getLeader(shard: Shard) : Address {
        if (zk.existsZNodeData(leadersIpPath + shard.toString())) {
            return zk.getZNodeData(leadersIpPath, false) as String
        }
        throw Exception("No leader")
    }

    fun getTimestamp() : TimeStamp {
        val timeStamp : TimeStamp
        if (zk.existsZNodeData(globalClockPath)) {
            timeStamp = (zk.getZNodeData(globalClockPath, false) as String).toLong()+1
            zk.updateZNodeData(globalClockPath, (timeStamp).toString().toByteArray())
        } else {
            timeStamp = 0
            zk.createZNodeData(globalClockPath, timeStamp.toString().toByteArray(), CreateMode.PERSISTENT)
        }
        return timeStamp
    }*/

    /*private fun run(args:Array<String> = emptyArray(), the_main: GetTimestamp) : TimeStamp {
        return runBlocking {
            BasicConfigurator.configure()

            val zkSockets = (1..3).map { Pair("127.0.0.1", 2180 + it) }
            val zkConnectionString = makeConnectionString(zkSockets)

            return@runBlocking withZooKeeperLong(zkConnectionString) {
                return@withZooKeeperLong the_main(args, it)
            }
        }
    }

    fun getTS() : TimeStamp {
        return run { _, zk ->
            val seqNum = zk.create(globalClockPath+"ts-") {
                flags = Ephemeral and Sequential
            }.first.let { ZKPaths.extractSequentialSuffix(it)!! }
            return@run seqNum.toLong()
        }
    }*/

    fun getIp(): String {
        return InetAddress.getLocalHost().hostAddress
    }

    private fun initialize() {
        zkConnection = ZkConnection()
        val zk = zkConnection.connect("host.docker.internal"/*"localhost"*/)
        this.zk = ZookeeperKtClient(zk)
    }

    private fun initMembers() {
        runBlocking {
            queryMembershipJob = launch {
                membership = Membership.make(zk, "shards")

                chan = Channel()
                membership.onChange = {
                    chan.send(membership.queryMembers())
                }
            }
        }
    }

    fun initMutex(): ZKMutex {
        return runBlocking {
            return@runBlocking ZKMutex.make(zk, "tx-lock")
        }
    }

    fun transfer(address: Address, amount: Value) : cs236351.txservice.Transfer {
        return cs236351.txservice.Transfer.newBuilder().setAddress(address).setAmount(amount).build()
    }

    fun trRequest(source: Address, txId: Id, tr: Transfer) : TrRequest {
        return TrRequest.newBuilder().setSource(source).setTxId(TxId.newBuilder().setId(txId).build()).setTr(
            transfer(tr.address, tr.amount)).build()
    }

    private fun trToLogEntry(trRequest: TrRequest) = "${logPath}/${trRequest.source}_${trRequest.txId.id}_${trRequest.tr.address}_${trRequest.tr.amount}"
    private fun logEntryToTr(logEntry: String): TrRequest {
        val entryComponents = logEntry.split('_')
        val txId = entryComponents[1].toLong()
        val tr = Transfer(entryComponents[0], entryComponents[2], entryComponents[3].toLong())
        return trRequest(tr.source, txId, tr)
    }

    init {
        initialize()
        initMembers()
        //initMutex()
        //join()
        /*val chan = Channel<Unit>()
        val zk = ZooKeeper("localhost:9000", 1000) { event ->
            if (event.state == Watcher.Event.KeeperState.SyncConnected &&
                event.type == Watcher.Event.EventType.None
            ) {
                runBlocking { chan.send(Unit) }
            }
        }
        this.zk = ZookeeperKtClient(zk)
        //chan.receive()*/
    }

    fun getTimestamp() : TimeStamp {
        val seqNum = runBlocking {
            zk.create(globalClockPath + "ts-") {
                flags = Ephemeral and Sequential
            }.first.let { ZKPaths.extractSequentialSuffix(it) }
        }
        return seqNum.toLong()
    }

    fun incLogTransfer(trRequest: TrRequest) : TimeStamp {
        var ts = 0
        runBlocking {
            if (zk.exists(trToLogEntry(trRequest)).first) {
                ts = zk.getZNodeData(trToLogEntry(trRequest), false).toString().toInt() + 1
                zk.updateZNodeData(trToLogEntry(trRequest), ts.toString().toByteArray())
            }
        }
        return ts.toLong()
    }

    fun resetLogTransfer(trRequest: TrRequest) {
        runBlocking {
            if (zk.exists(trToLogEntry(trRequest)).first) {
                zk.updateZNodeData(trToLogEntry(trRequest), 0.toString().toByteArray())
            }
        }
    }

    fun logTransfer(trRequest: TrRequest) {
        val seqNum = runBlocking {
            zk.create(trToLogEntry(trRequest)) {
                data = 0.toString().toByteArray()
                flags = Ephemeral
            }.first.let { ZKPaths.extractSequentialSuffix(it) }
        }
    }

    fun logTransferEntry(logEntry: String) {
        val seqNum = runBlocking {
            zk.create(logEntry) {
                data = 0.toString().toByteArray()
                flags = Ephemeral
            }.first.let { ZKPaths.extractSequentialSuffix(it) }
        }
    }

    fun commitTransfer(trRequest: TrRequest) {
        runBlocking {
            zk.delete(trToLogEntry(trRequest))
        }
    }

    fun commitTransferEntry(logEntry: String) {
        runBlocking {
            zk.delete(logEntry)
        }
    }

    private suspend fun getLog(): List<String> = zk.getChildren(logPath).first

    fun queryLog() {
        runBlocking {
            while (true) {
                delay(10_000)
                if (ShardsRepository.leader()) {
                    val entries = getLog()
                    for (entry in entries) {
                        val trRequest = logEntryToTr(entry)
                        val time = incLogTransfer(trRequest)
                        if (time > 3) {
                            TxClient.sendTr(trRequest.txId.id, trRequest.source, trRequest.tr)
                            println("##### ${trRequest.txId.id}-${trRequest.tr.address} timeout!!! #####")
                            resetLogTransfer(trRequest)
                        }
                    }
                }
            }
        }
    }

    fun join() {
        /*runBlocking {
            launch { membership.join(getIp()) }
            launch { queryMembers() }
        }*/
        CoroutineScope(Dispatchers.IO).launch {
            runCatching{
                membership.join(ShardsRepository.getShardIpName())
                //queryMembers()
            }
        }
    }

    fun queryMembers() {
        runBlocking {
            val job = launch {
                for (members in chan) {
                    ShardsRepository.setIps(members)
                    println("Members: ${members.joinToString(", ")}")
                }
            }
            chan.send(membership.queryMembers())
            job.join()
        }
    }

    fun txLock(mutex: ZKMutex) {
        runBlocking {
            try {
                mutex.lock()
            } catch (e: IllegalStateException) {
                println(e)
            }
        }
    }

    fun txUnlock(mutex: ZKMutex) {
        runBlocking { mutex.unlock() }
    }

    fun utxoLock(address: Address) : ZKMutex {
        return runBlocking {
            val utxoMutex = ZKMutex.make(zk, address)
            try {
                utxoMutex.lock()
            } catch (e: IllegalStateException) {
                println(e)
            }
            return@runBlocking utxoMutex
        }
    }

    fun utxoUnlock(utxoMutex: ZKMutex) {
        runBlocking { utxoMutex.unlock() }
    }
}
