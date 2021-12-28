package zk_service

import org.apache.zookeeper.CreateMode

object ZkClient {
    @JvmStatic
    fun main(args: Array<String>) {
        val zk = ZookeeperKtClient()
        val exists = zk.existsZNodeData("/n1")
        println("*** $exists ***")
        if(exists) zk.deleteZNodeData("/n1")
        zk.createZNodeData("/n1", "data1".toByteArray(), CreateMode.PERSISTENT)
        var data = zk.getZNodeData("/n1", false)
        println(data)
        data = zk.getZNodeData("/n1", false)
        println(data)
        zk.updateZNodeData("/n1", "data3".toByteArray())
        data = zk.getZNodeData("/n1", false)
        println(data)
    }
}