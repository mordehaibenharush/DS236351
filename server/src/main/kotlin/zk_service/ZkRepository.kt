/*
package zk_service

import grpc_service.Address
import grpc_service.Shard
import org.apache.zookeeper.CreateMode

typealias TimeStamp = Long

class ZkRepository {
    private var zk : ZookeeperKtClient = ZookeeperKtClient(null)
    private val shardsPath : Array<String> = arrayOf("1",)
    private val globalClockPath : String = "/clock"
    private val leadersIpPath : String = "/leaders"

    fun updateLeader(shard: Shard, address: Address) {
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
    }
}*/
