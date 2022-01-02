package com.example.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import kotlinx.coroutines.*
import grpc_service.TxServer
import kotlinx.coroutines.CoroutineScope

@SpringBootApplication
class Main

fun main(args: Array<String>) {
	CoroutineScope(Dispatchers.IO).launch {
		runCatching{
			TxServer.main(args)
		}
	}
	runApplication<Main>(*args)
}
