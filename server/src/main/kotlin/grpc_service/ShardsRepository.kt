package grpc_service

import cs236351.txservice.TxServiceGrpc
import zk_service.ZChildren
import java.net.InetAddress

//import zk_service.ZkRepository

enum class Shard {SHARD1, SHARD2}

object ShardsRepository {
    //private var zk: ZkRepository = ZkRepository()
    val ips: Map<Shard, ArrayList<Address>> = mapOf(Shard.SHARD1 to ArrayList(), Shard.SHARD2 to ArrayList())
    //val ips: Array<String> = arrayOf("cluster1.clusters.local", "cluster2.clusters.local")
    private val shardNum = System.getenv("SHARD").toInt()
    private val shard = Shard.values()[shardNum]

    fun getShardIpName() = "${shardNum}-${InetAddress.getLocalHost().hostAddress}"
    fun getShardFromName(name: String) = Shard.values()[name.split('-')[0].toInt()]
    fun getIpFromName(name: String) = name.split('-')[1]

    fun setIps(members: ZChildren) {
        for (member in members) {
            val shard = getShardFromName(member)
            val ip = getIpFromName(member)
            ips[shard]!!.clear()
            ips[shard]!!.add(ip)
            ips[shard]!!.sortBy { it }
            println(ips[shard])
        }
    }

    fun getShardLeaderIp(address: Address) : Address {
        val shard = Shard.values()[address.split('.').last().toInt().mod(Shard.values().size)]
        return ips[shard]!![0]
    }
}