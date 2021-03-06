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
			launch { zk.queryMembers() }
			val broadcast = BroadcastServiceImpl
			launch { broadcast.start(-1) }
			launch { zk.queryLog() }
		}
	}
	/*CoroutineScope(Dispatchers.IO).launch {
		runCatching{
			val zk = ZkRepository
			launch { zk.queryMembers() }
		}
	}
	CoroutineScope(Dispatchers.IO).launch {
		runCatching{
			//delay(5_000)
			val broadcast = BroadcastServiceImpl
			launch { broadcast.start(-1) }
		}
	}*/
	CoroutineScope(Dispatchers.IO).launch {
		runCatching{
			TxServer.main(args)
		}
	}
	/*CoroutineScope(Dispatchers.IO).launch {
		runCatching{
			//val zk = ZkRepository
			//launch { zk.queryLog() }
		}
	}*/
	runApplication<Main>(*args)
}
