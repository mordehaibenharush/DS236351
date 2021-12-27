package grpc_service

import io.grpc.stub.StreamObserver
import cs236351.txservice.HelloReply
import cs236351.txservice.HelloRequest
import cs236351.txservice.MyServiceGrpc.MyServiceImplBase

class GrpcServiceImpl : MyServiceImplBase() {
    override fun sayHello(
        request: HelloRequest, responseObserver: StreamObserver<HelloReply?>
    ) {
        val greeting: String = StringBuilder()
            .append("Hello, ")
            .append(request.getName())
            .toString()
        val response: HelloReply = HelloReply.newBuilder().setMessage(greeting)
            .build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}