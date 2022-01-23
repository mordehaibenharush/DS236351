package zk_service

import kotlinx.coroutines.*
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.data.Stat
import java.util.concurrent.Executors
import org.apache.zookeeper.Watcher
import java.nio.charset.Charset

typealias Path = String
typealias ZName = String
typealias ZChildren = List<ZName>




class ZookeeperKtClient(private val zk: ZooKeeper) : ZooKeeperKt {
    override val namespace: Path
        get() = "/"

    override suspend fun create(op: CreateOperation): Pair<Path, Stat> {
        val path = applyNamespace(op.path, op.flags.isSequential)
        val mode = op.flags.zkCreateMode
        val stat = Stat()
        var createdPath: Path = ""

        try {
            /*if (!exists(path).first)
            return Pair(createdPath, stat)*/

            if (op.flags.hasTTL) {
                val ttl = op.flags.TTL
                createdPath = zkThreadContext {
                    catchKeeperExceptions(op.handlers) {
                        zk.create(path, op.data, op.acl, mode, stat, ttl)!!
                    } ?: "EMPTY"
                }
            } else {
                createdPath = zkThreadContext {
                    catchKeeperExceptions(op.handlers) {
                        //println("creating $path")
                        val p = zk.create(path, op.data, op.acl, mode, stat)
                        //println("created $p")
                        p!!
                    } ?: "EMPTY"
                }
            }
        } catch (e: Throwable) {
            println(e)
        }
        return Pair(createdPath, stat)
    }

    override suspend fun getChildren(op: GetChildrenOperation): Pair<ZChildren, Stat> {
        val path = applyNamespace(op.path)
        val stat = Stat()
        val watcher: Watcher? = op.watchers.all?.toZKWatcher()
        val childrenList = zkThreadContext {
            catchKeeperExceptions(op.handlers) {
                zk.getChildren(path, watcher, stat)
            }
        }
        return Pair(childrenList!!.map { it!! }, stat)
    }

    override suspend fun delete(op: DeleteOperation) {
        val path = applyNamespace(op.path)
        val version: Int = op.version ?: -1
        //if (exists(path).first) {
        zkThreadContext {
            catchKeeperExceptions(op.handlers) {
                zk.delete(path, version)
            }
        }
    //}
    }

    override suspend fun exists(op: CheckExistenceOperation): Pair<Boolean, Stat?> {
        val path = applyNamespace(op.path)
        val watcher: Watcher? = op.watchers.all?.toZKWatcher()
        var stat : Stat? = null
        kotlin.runCatching {
            stat = zkThreadContext {
                catchKeeperExceptions(op.handlers) {
                    zk.exists(path, watcher)
                }
            }
        }
        return Pair(stat != null, stat)
    }

    @Throws(KeeperException::class, InterruptedException::class)
    fun getZNodeData(path: String?, watchFlag: Boolean): Any? {
        try {
            val b: ByteArray? = zk!!.getData(path, watchFlag, Stat())
            return Charset.availableCharsets()["UTF-8"]?.let { String(b!!, it) }
        } catch (e: Throwable) {
            println(e)
            return "0"
        }
    }

    fun updateZNodeData(path: String?, data: ByteArray?) {
        try {
            val version = zk!!.exists(path, true).version
            zk!!.setData(path, data, version)
        } catch (e: Throwable) {
            println(e)
        }
    }

    fun existsZNodeData(path: String?): Boolean {
        return (zk!!.exists(path, false) != null);
    }

    @Throws(KeeperException::class, InterruptedException::class)
    fun deleteZNodeData(path: String?) {
        zk!!.delete(
            path, -1
        )
    }

}


private val zkAPIContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

private suspend fun <T> zkThreadContext(block: () -> T): T = coroutineScope {
    async(zkAPIContext) { block() }.await()
}

/*
package zk_service

import kotlinx.coroutines.*
import org.apache.zookeeper.*
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.data.Stat
import java.nio.charset.Charset
import java.util.concurrent.Executors

typealias Path = String
typealias ZName = String
typealias ZChildren = List<ZName>




class ZookeeperKtClient(zk: ZooKeeper?) : ZooKeeperKt {
    override val namespace: Path
        get() = "/"

    */
/*private fun initialize() {
        zkConnection = ZkConnection()
        zk = zkConnection!!.connect("host.docker.internal"*//*
*/
/*"navigator.zk.local"*//*
*/
/*)
    }

    fun closeConnection() {
        zkConnection!!.close()
    }*//*


    override suspend fun create(op: CreateOperation): Pair<Path, Stat> {
        val path = applyNamespace(op.path, op.flags.isSequential)
        val mode = op.flags.zkCreateMode
        val stat = Stat()
        var createdPath: Path = ""

        if (op.flags.hasTTL) {
            val ttl = op.flags.TTL
            createdPath = zkThreadContext {
                catchKeeperExceptions(op.handlers) {
                    zk?.create(path, op.data, op.acl, mode, stat, ttl)!!
                } ?: "EMPTY"
            }
        } else {
            createdPath = zkThreadContext {
                catchKeeperExceptions(op.handlers) {
                    println("creating $path")
                    val p= zk?.create(path, op.data, op.acl, mode, stat)
                    println("created $p")
                    p!!
                } ?: "EMPTY"
            }
        }
        return Pair(createdPath, stat)
    }

    override suspend fun getChildren(op: GetChildrenOperation): Pair<ZChildren, Stat> {
        val path = applyNamespace(op.path)
        val stat = Stat()
        val watcher: Watcher? = op.watchers.all?.toZKWatcher()
        val childrenList = zkThreadContext {
            catchKeeperExceptions(op.handlers) {
                zk?.getChildren(path, watcher, stat)
            }
        }
        return Pair(childrenList!!.map { it!! }, stat)
    }

    override suspend fun delete(op: DeleteOperation) {
        val path = applyNamespace(op.path)
        val version: Int = op.version ?: -1
        zkThreadContext {
            catchKeeperExceptions(op.handlers) {
                zk?.delete(path, version)
            }
        }
    }

    override suspend fun exists(op: CheckExistenceOperation): Pair<Boolean, Stat?> {
        val path = applyNamespace(op.path)
        val watcher: Watcher? = op.watchers.all?.toZKWatcher()
        val stat = zkThreadContext {
            catchKeeperExceptions(op.handlers) {
                zk?.exists(path, watcher)
            }
        }
        return Pair(stat != null, stat)
    }

    @Throws(KeeperException::class, InterruptedException::class)
     fun createZNodeData(path: String?, data: ByteArray?, createMode: CreateMode) {
        zk!!.create(
            path,
            data,
            ZooDefs.Ids.OPEN_ACL_UNSAFE,
            createMode
        )
    }

    @Throws(KeeperException::class, InterruptedException::class)
    fun deleteZNodeData(path: String?) {
        zk!!.delete(
            path, -1
        )
    }

    @Throws(KeeperException::class, InterruptedException::class)
    fun getZNodeData(path: String?, watchFlag: Boolean): Any? {
        val b: ByteArray? = zk!!.getData(path, watchFlag, Stat())
        return Charset.availableCharsets()["UTF-8"]?.let { String(b!!, it) }
    }

    @Throws(KeeperException::class, InterruptedException::class)
    fun updateZNodeData(path: String?, data: ByteArray?) {
        val version = zk!!.exists(path, true).version
        zk!!.setData(path, data, version)
    }

    fun existsZNodeData(path: String?): Boolean {
        return (zk!!.exists(path, false) != null);
    }

    companion object {
        private var zk: ZooKeeper? = null
        private var zkConnection: ZkConnection? = null
    }

    */
/*init {
        initialize()
    }*//*

}


private val zkAPIContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

private suspend fun <T> zkThreadContext(block: () -> T): T = coroutineScope {
    async(zkAPIContext) { block() }.await()
}

*/
