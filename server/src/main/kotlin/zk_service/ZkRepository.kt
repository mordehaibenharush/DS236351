package zk_service

import grpc_service.Address
import grpc_service.Shard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.apache.log4j.BasicConfigurator
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.ZooKeeper
import org.springframework.web.servlet.function.ServerResponse.async

typealias TimeStamp = Long
//typealias GetTimestamp = suspend CoroutineScope.(Array<String>, client: ZooKeeperKt) -> TimeStamp

fun main(args: Array<String>) {
    val tsList = ArrayList<TimeStamp>()
    for (i in 0..9) {
        val ts =  ZkRepository.getTimestamp()
        tsList.add(ts)
    }
    println(tsList)
}

object ZkRepository {
    lateinit var zk : ZookeeperKtClient
    lateinit var zkConnection: ZkConnection
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

    private fun initialize() {
        zkConnection = ZkConnection()
        val zk = zkConnection!!.connect("host.docker.internal"/*"localhost"*/)
        this.zk = ZookeeperKtClient(zk)
    }

    init {
        initialize()
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
            }.first.let { ZKPaths.extractSequentialSuffix(it)!! }
        }
        return seqNum.toLong()
    }
}
