package grpc_service

import com.google.protobuf.Empty
import cs236351.txservice.*
import io.grpc.stub.StreamObserver
import cs236351.txservice.TxServiceGrpc.TxServiceImplBase

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

    override fun getAllTx(request: Request, responseObserver: StreamObserver<TransactionList>) {
        val transactionListBuilder : TransactionList.Builder = TransactionList.newBuilder()
        transactionListBuilder.addAllTxList(transactionRepository.getTxList(request.address))
        val response: TransactionList = transactionListBuilder.build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun sendTr(request: TrRequest, responseObserver: StreamObserver<Empty>) {
        val res = transactionRepository.removeUtxoByValue(request.source, request.tr.amount)
        if (res)
            transactionRepository.insertUtxo(TxClient.utxo(-1, request.tr.address, request.tr.amount))
        responseObserver.onNext(TxClient.empty())
        responseObserver.onCompleted()
    }

    override fun getAllUtxo(request: Request, responseObserver: StreamObserver<UtxoList>) {
        val utxoListBuilder : UtxoList.Builder = UtxoList.newBuilder()
        utxoListBuilder.addAllUtxoList(transactionRepository.getUtxos(request.address))
        val response: UtxoList = utxoListBuilder.build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }

    override fun getLedger(request: Limit?, responseObserver: StreamObserver<TransactionList>) {
        val transactionListBuilder : TransactionList.Builder = TransactionList.newBuilder()
        transactionListBuilder.addAllTxList(transactionRepository.getLedger(null))
        val response: TransactionList = transactionListBuilder.build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}