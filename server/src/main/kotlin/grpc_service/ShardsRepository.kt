package grpc_service

import cs236351.txservice.TxServiceGrpc
import zk_service.ZkRepository

enum class Shard {SHARD1, SHARD2}

object ShardsRepository {
    private var zk: ZkRepository = ZkRepository()
    val ips: Array<String> = arrayOf("cluster1.clusters.local", "cluster2.clusters.local")

    fun getShardLLeaderIp(address: Address) : Address {
        if (address in ips)
            return address
        val shardIndx = address.split('.').last().toInt().mod(Shard.values().size)
        return ips[shardIndx]
    }
}