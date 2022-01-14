/*
package zk_service

import cs236351.txservice.TrRequest
import cs236351.txservice.Transfer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

fun main(args: Array<String>) = mainWith(args) { _, zk ->
    val mem = Membership.make(zk, args[0])

    val chan = Channel<ZChildren>()
    mem.onChange = {
        chan.send(mem.queryMembers())
    }
    val task = launch {
        for (members in chan) {
            println("Members: ${members.joinToString(", ")}")
        }
    }

    chan.send(mem.queryMembers())
    mem.join(args[1])
    task.join()
}

class State private constructor(private val zk: ZooKeeperKt) {
    var _id: String? = null
    val id: String get() = _id!!

    var onChange: (suspend () -> Unit)? = null

    suspend fun logTransfer(trRequest: TrRequest) {
        val (_, stat) = zk.create {
            path = "/${trRequest.tr.address}_${trRequest.txId}"
            data =
            flags = Persistent
        }
        _id = id
    }

    suspend fun commitTransfer(trRequest: TrRequest) {
        zk.delete("/${trRequest.tr.address}_${trRequest.txId}")
    }

    suspend fun queryMembers(): List<String> = zk.getChildren("/") {
        watchers += this@State.onChange?.let { { _, _, _ -> it() } }
    }.first

    suspend fun leave() {
        zk.delete("/$id")
    }

    companion object {
        suspend fun make(zk: ZooKeeperKt, groupName: String): Membership {
            val zk = zk.usingNamespace("/membership")
                .usingNamespace("/$groupName")
            return Membership(zk, groupName)
        }
    }
}*/
