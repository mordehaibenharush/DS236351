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




class ZookeeperKtClient() : ZooKeeperKt {
    override val namespace: Path
        get() = "/"

    private fun initialize() {
        zkConnection = ZkConnection()
        zk = zkConnection!!.connect("localhost")
    }

    fun closeConnection() {
        zkConnection!!.close()
    }

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
        var b: ByteArray?
        b = zk!!.getData(path, watchFlag, Stat())
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

    init {
        initialize()
    }
}


private val zkAPIContext = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

private suspend fun <T> zkThreadContext(block: () -> T): T = coroutineScope {
    async(zkAPIContext) { block() }.await()
}

