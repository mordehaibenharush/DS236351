package zk_service

import grpc_service.Address
import grpc_service.Shard
import grpc_service.ShardsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.InetAddress

typealias TimeStamp = Long
//typealias GetTimestamp = suspend CoroutineScope.(Array<String>, client: ZooKeeperKt) -> TimeStamp

fun main(args: Array<String>) {
    val zkr = ZkRepository
    runBlocking {
            zkr.txLock()
            println(args[0])
            delay(20000)
            zkr.txUnlock()
            println("${args[0]} unlocked")
        /*launch {
            zkr.lock()
            println("22222222")
            delay(10000)
            zkr.unlock()
            println("222 unlocked")
        }*/
    }
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
    private val leadersIpPath : String = "/leaders/"

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

    private fun initMembers(shard: Shard) {
        runBlocking {
            queryMembershipJob = launch {
                membership = Membership.make(zk, shard.toString())

                chan = Channel()
                membership.onChange = {
                    chan.send(membership.queryMembers())
                }
            }
        }
    }

    private fun initMutex() {
        runBlocking {
            txMutex = ZKMutex.make(zk, "tx-lock")
        }
    }

    init {
        initialize()
        initMembers(Shard.SHARD1)
        initMutex()
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

    fun join() {
        /*runBlocking {
            launch { membership.join(getIp()) }
            launch { queryMembers() }
        }*/
        CoroutineScope(Dispatchers.IO).launch {
            runCatching{
                membership.join(ShardsRepository.getShardIpName())
                queryMembers()
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

    fun txLock() {
        runBlocking {
            try {
                txMutex.lock()
            } catch (e: IllegalStateException) {
                println(e)
            }
        }
    }

    fun txUnlock() {
        runBlocking { txMutex.unlock() }
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
