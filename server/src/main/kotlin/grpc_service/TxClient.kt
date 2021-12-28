package grpc_service

import com.google.protobuf.Empty
import cs236351.txservice.*
import io.grpc.ManagedChannelBuilder

object TxClient {
    @JvmStatic
    fun main(args: Array<String>) {
        val channel = ManagedChannelBuilder.forAddress("localhost", 8090)
            .usePlaintext()
            .build()
        val stub: GrpcServiceGrpc.GrpcServiceBlockingStub = GrpcServiceGrpc.newBlockingStub(channel)
        val txId : TxId = TxId.newBuilder().setId(1).build()
        val tx : Transaction = Transaction.newBuilder()
            .setTxId(txId)
            .addInputs(Utxo.newBuilder().setTxId(txId).setAddress("1"))
            .addOutputs(Transfer.newBuilder().setAddress("2").setAmount(3))
            .build()
        stub.insertTx(tx)
        val response : TransactionList = stub.getAllTx(Empty.newBuilder().build())
        println(response)
        channel.shutdown()
    }
}