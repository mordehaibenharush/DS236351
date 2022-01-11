package grpc_service

import cs236351.txservice.TxServiceGrpc
import multipaxos.ID
import zk_service.ZChildren
import java.net.InetAddress

//import zk_service.ZkRepository

enum class Shard {SHARD1, SHARD2}

fun main() {
    val s = ShardsRepository
    s.setIps(listOf("0-0.0.0.0", "0-0.0.0.1", "0-0.0.0.2", "1-0.0.0.3"))
    println(s.getShardLeaderId("0.0.0.2"))
    println(s.getShardLeaderId("0.0.0.3"))
    println(s.getId("0.0.0.3"))
    println(s.getIpFromId(2))
}

object ShardsRepository {
    //private var zk: ZkRepository = ZkRepository()
    val ips: Map<Shard, ArrayList<Address>> = mapOf(Shard.SHARD1 to ArrayList(), Shard.SHARD2 to ArrayList())
    //val ips: Array<String> = arrayOf("cluster1.clusters.local", "cluster2.clusters.local")
    private val shardNum = System.getenv("SHARD").toInt()
    private val shard = Shard.values()[shardNum]

    fun getShardIpName() = "${shardNum}-${InetAddress.getLocalHost().hostAddress}"
    fun getShardFromName(name: String) = Shard.values()[name.split('-')[0].toInt()]
    fun getIpFromName(name: String) = name.split('-')[1]
    fun getIp(): String {
        return InetAddress.getLocalHost().hostAddress
    }
    fun getId(address: Address) : ID {
        return address.split('.').last().toInt()
    }

    fun setIps(members: ZChildren) {
        ips.forEach { l -> l.value.clear() }
        for (member in members) {
            val shard = getShardFromName(member)
            val ip = getIpFromName(member)
            ips[shard]!!.add(ip)
            ips[shard]!!.sortBy { it }
            println(ips[shard])
        }
    }

    fun getShardLeaderIp(address: Address) : Address {
        val shard = Shard.values()[address.split('.').last().toInt().mod(Shard.values().size)]
        return ips[shard]!![0]
    }

    fun getShardLeaderId(address: Address) : ID {
        //val shard = Shard.values()[getId(address).mod(Shard.values().size)]
        return ips[shard]!![0].split('.').last().toInt()
    }

    fun getShardLeaderIpFromId(id: ID) : Address {
        //val shard = Shard.values()[id.mod(Shard.values().size)]
        return ips[shard]!![0]
    }

    fun getIpFromId(id: ID) : Address {
        //val shard = Shard.values()[id.mod(Shard.values().size)]
        return ips[shard]!!.filter { getId(it) == id }[0]
    }

    fun getShardIps(id: ID) : ArrayList<Address>? {
        //val shard = Shard.values()[id.mod(Shard.values().size)]
        return ips[shard]
    }

    fun getShardIds(id: ID) : List<ID>? {
        //val shard = Shard.values()[id.mod(Shard.values().size)]
        return ips[shard]?.map { getId(it) }
    }
}