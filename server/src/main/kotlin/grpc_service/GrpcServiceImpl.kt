package grpc_service

import com.google.protobuf.Empty
import cs236351.txservice.Exists
import io.grpc.stub.StreamObserver
import cs236351.txservice.Transaction
import cs236351.txservice.TxServiceGrpc.TxServiceImplBase
import cs236351.txservice.TransactionList
import cs236351.txservice.TxId

class GrpcServiceImpl : TxServiceImplBase() {
    private val transactionRepository: TransactionRepository = TransactionRepository()

    override fun insertTx(request: Transaction, responseObserver: StreamObserver<Empty>) {
        transactionRepository.insertTx(request)
        responseObserver.onNext(Empty.newBuilder().build())
        responseObserver.onCompleted()
    }

    override fun existsTx(request: TxId, responseObserver: StreamObserver<Exists>) {
        val response: Exists = Exists.newBuilder().setExists(transactionRepository.existsTx(request)).build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getTx(request: TxId, responseObserver: StreamObserver<Transaction?>) {
        val response: Transaction? = transactionRepository.getTx(request)
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getAllTx(request: Empty, responseObserver: StreamObserver<TransactionList>) {
        var transactionListBuilder : TransactionList.Builder = TransactionList.newBuilder()
        transactionListBuilder.addAllTxList(transactionRepository.getTxMap())
        val response: TransactionList = transactionListBuilder.build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}