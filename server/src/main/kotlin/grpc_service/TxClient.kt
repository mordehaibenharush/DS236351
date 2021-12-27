package grpc_service

import io.grpc.ManagedChannelBuilder
import cs236351.txservice.HelloReply
import cs236351.txservice.HelloRequest
import cs236351.txservice.MyServiceGrpc

object TxClient {
    @JvmStatic
    fun main(args: Array<String>) {
        val channel = ManagedChannelBuilder.forAddress("localhost", 8090)
            .usePlaintext()
            .build()
        val stub: MyServiceGrpc.MyServiceBlockingStub = MyServiceGrpc.newBlockingStub(channel)
        val helloResponse: HelloReply = stub.sayHello(
            HelloRequest.newBuilder()
                .setName("Baeldung")
                .build()
        )
        System.out.println(helloResponse)
        channel.shutdown()
    }
}