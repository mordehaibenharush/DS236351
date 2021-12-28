package zk_service

import org.apache.zookeeper.Watcher.Event.KeeperState
import org.apache.zookeeper.ZooKeeper
import java.io.IOException
import java.util.concurrent.CountDownLatch


class ZkConnection {
    private var zoo: ZooKeeper? = null
    var connectionLatch = CountDownLatch(1)

    // ...
    @Throws(IOException::class, InterruptedException::class)
    fun connect(host: String?): ZooKeeper {
        zoo = ZooKeeper(host, 9000) { we ->
            if (we.state == KeeperState.SyncConnected) {
                connectionLatch.countDown()
            }
        }
        connectionLatch.await()
        return zoo as ZooKeeper
    }

    @Throws(InterruptedException::class)
    fun close() {
        zoo!!.close()
    }
}