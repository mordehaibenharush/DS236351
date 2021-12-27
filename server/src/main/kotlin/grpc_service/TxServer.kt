package grpc_service

import io.grpc.Server
import io.grpc.ServerBuilder
import java.io.IOException


object TxServer {
    @Throws(InterruptedException::class, IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val server: Server = ServerBuilder
            .forPort(8090)
            .addService(GrpcServiceImpl()).build()
        server.start()
        server.awaitTermination()
    }
}
