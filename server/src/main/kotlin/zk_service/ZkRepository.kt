package zk_service

import org.apache.zookeeper.CreateMode

typealias TimeStamp = Long
enum class shards {SHARD1}

class ZkRepository {
    private var zk : ZookeeperKtClient = ZookeeperKtClient()
    private val shardsPath : Array<String> = arrayOf("1",)
    private val globalClockPath : String = "/clock"

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
    }
}