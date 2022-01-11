package com.example.api

import grpc_service.ShardsRepository.getIp
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlinx.coroutines.*
import grpc_service.TxServer
import kotlinx.coroutines.CoroutineScope
import multipaxos.BroadcastServiceImpl
import zk_service.ZkRepository

@SpringBootApplication
class Main

fun main(args: Array<String>) {
	CoroutineScope(Dispatchers.IO).launch {
		runCatching{
			val zk = ZkRepository
			launch { zk.join() }
		}
	}
	CoroutineScope(Dispatchers.IO).launch {
		runCatching{
			val zk = ZkRepository
			launch { zk.queryMembers() }
		}
	}
	CoroutineScope(Dispatchers.IO).launch {
		runCatching{
			val broadcast = BroadcastServiceImpl
			launch { broadcast.start(-1) }
		}
	}
	/*runBlocking {
		delay(10000)
		val ip = getIp()
		for (i in 0..9)
			BroadcastServiceImpl.send("*** $ip - $i ***")
	}*/
	CoroutineScope(Dispatchers.IO).launch {
		runCatching{
			TxServer.main(args)
		}
	}
	runApplication<Main>(*args)
}
