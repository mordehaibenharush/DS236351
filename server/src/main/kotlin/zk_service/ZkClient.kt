package zk_service

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.WatchedEvent

object ZkClient {
    @JvmStatic
    fun main(args: Array<String>) {
        val zk = ZookeeperKtClient()
        val watcher : Watcher = {path: Path, type: org.apache.zookeeper.Watcher.Event.EventType, state: org.apache.zookeeper.Watcher.Event.KeeperState -> println("*** $path ***") }
        val job = runBlocking { launch {watcher.invoke("n2", org.apache.zookeeper.Watcher.Event.EventType.NodeChildrenChanged, org.apache.zookeeper.Watcher.Event.KeeperState.ConnectedReadOnly)}}
        job.start()
        val v = WatchedEvent(org.apache.zookeeper.Watcher.Event.EventType.NodeChildrenChanged, org.apache.zookeeper.Watcher.Event.KeeperState.ConnectedReadOnly, "/n2")
        /*val job = runBlocking { launch { zk = zk.usingNamespace("/") as ZookeeperKtClient }}
        val op1 = CreateOperation(zk.namespace+"path1", CreateFlags.Ephemeral, "hello".toByteArray())
        val job1 = runBlocking { launch { zk.create(op1) } }
        print(job1)
        val op2 = GetChildrenOperation("path1", )
        val job2 = runBlocking { launch { zk.getChildren(op2) } }
        print(job2)*/
        val exists = zk.existsZNodeData("/n2")
        println("*** $exists ***")
        if(exists) zk.deleteZNodeData("/n2")
        zk.createZNodeData("/n2", "data1".toByteArray(), CreateMode.PERSISTENT)
        var data = zk.getZNodeData("/n2", true)
        println(data)
        zk.updateZNodeData("/n2", "data3".toByteArray())
        data = zk.getZNodeData("/n2", false)
        println(data)
    }
}