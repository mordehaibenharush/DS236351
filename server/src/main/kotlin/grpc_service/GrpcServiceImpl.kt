package grpc_service

import com.google.protobuf.Empty
import cs236351.txservice.*
import io.grpc.stub.StreamObserver
import cs236351.txservice.TxServiceGrpc.TxServiceImplBase
import multipaxos.BroadcastServiceImpl
import multipaxos.msgType
import zk_service.ZkRepository

object GrpcServiceImpl : TxServiceImplBase() {
    private val transactionRepository = TransactionRepository

    fun executeTx(tx: Transaction) {
        val tx = TxClient.transaction(ZkRepository.getTimestamp(), tx.inputsList, tx.outputsList)
        BroadcastServiceImpl.send(msgType.INSERT_TRANSACTION, BroadcastServiceImpl.transactionToMsg(tx))
        for (utxo in tx.inputsList) {
            BroadcastServiceImpl.send(msgType.SPEND_UTXO, BroadcastServiceImpl.utxoToMsg(utxo))
        }
        for (tr in tx.outputsList) {
            TxClient.addUtxo(tx.txId.id, tx.inputsList[0].address, tr)
        }
    }

    override fun insertTx(request: Transaction, responseObserver: StreamObserver<Reply>) {
        //ZkRepository.lock()
        var res = Ack.YES
        for (utxo in request.inputsList) {
            if (transactionRepository.spentUtxo(utxo.address, utxo.txId.id)) {
                res = Ack.NO
                break
            }
        }
        if (res == Ack.YES) {
            executeTx(request)
            /*BroadcastServiceImpl.send(msgType.INSERT_TRANSACTION, BroadcastServiceImpl.transactionToMsg(request))
            for (utxo in request.inputsList) {
                BroadcastServiceImpl.send(msgType.SPEND_UTXO, BroadcastServiceImpl.utxoToMsg(utxo))
            }
            for (tr in request.outputsList) {
                TxClient.addUtxo(request.txId.id, request.inputsList[0].address, tr)
            }*/
        }
        //ZkRepository.unlock()
        responseObserver.onNext(Reply.newBuilder().setAck(res).build())
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

    override fun sendTr(request: TrRequest, responseObserver: StreamObserver<Reply>) {
        var res = Ack.NO
        /*var req = request
        if (request.txId.id == (-1).toLong())
            req = TxClient.trRequest(request.source, ZkRepository.getTimestamp(), request.tr)*/
        if (transactionRepository.getTotalUtxosValue(request.source) >= request.tr.amount) {
            BroadcastServiceImpl.send(msgType.DELETE_UTXO, BroadcastServiceImpl.transferToMsg(request))
            ZkRepository.logTransfer(request)
            //BroadcastServiceImpl.send(msgType.INSERT_UTXO, BroadcastServiceImpl.transferToMsg(request))
            //if (request.txId.id == (-1).toLong())
            TxClient.addUtxo(request.txId.id, request.tr.address, request.tr)
            res = Ack.YES
        }
        responseObserver.onNext(Reply.newBuilder().setAck(res).build())
        responseObserver.onCompleted()
    }

    override fun addUtxo(request: TrRequest, responseObserver: StreamObserver<Empty>) {
        BroadcastServiceImpl.send(msgType.INSERT_UTXO, BroadcastServiceImpl.transferToMsg(request))
        responseObserver.onNext(TxClient.empty())
        responseObserver.onCompleted()
    }

    override fun removeUtxo(request: TrRequest, responseObserver: StreamObserver<Empty>) {
        //if (/*request.txId.id == (-1).toLong()*/true)
        BroadcastServiceImpl.send(msgType.DELETE_UTXO, BroadcastServiceImpl.transferToMsg(request))
        /*else
            BroadcastServiceImpl.send(msgType.SPEND_UTXO, BroadcastServiceImpl.transferToMsg(request))*/
        //transactionRepository.removeUtxoByValue(request.source, request.tr.amount)
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

    override fun commitTr(request: TrRequest, responseObserver: StreamObserver<Reply>) {
        println("committing transfer ${request.txId}_${request.tr.address}")
        ZkRepository.commitTransfer(request)
        responseObserver.onNext(Reply.newBuilder().setAck(Ack.YES).build())
        responseObserver.onCompleted()
    }

    override fun insertTxList(request: TransactionList, responseObserver: StreamObserver<Empty>) {
        /*val totalBalance = transactionRepository.getTotalUtxosValue(request.txListList[0].inputsList[0].address)
        val totalInputsValue = request.txListList.fold(0.toLong())
            {total1, Tx -> total1 + (Tx.inputsList.fold(0.toLong()) {total2, utxo -> total2 + utxo.value}) }
        if (totalInputsValue < totalBalance) {
            //ZkRepository.txLock()
            for (tx in request.txListList) {
                transactionRepository.insertTx(tx)
                for (tr in tx.outputsList) {
                    TxClient.sendTr(tx.txId.id, tx.inputsList[0].address, tr)
                    *//*transactionRepository.removeUtxoByValue(request.inputsList[0].address, tr.amount)*//*
                }
            }
            //ZkRepository.txUnlock()
        }*/
        var commit = true
        val ip = ShardsRepository.getIp()
        for (tx in request.txListList) {
            println("${tx.inputsList[0].address} - ${ShardsRepository.getShardLeaderIp(tx.inputsList[0].address)}")
            if (ShardsRepository.getShardLeaderIp(tx.inputsList[0].address) == ip) {
                for (utxo in tx.inputsList) {
                    if (transactionRepository.spentUtxo(utxo.address, utxo.txId.id)) {
                        commit = false
                        //transactionRepository.rollbackTxList(request.txListList)
                        println("$ip - voting abort!")
                        ZkRepository.vote2pc(request.id, ip, "abort")
                        break
                    }
                    else {
                        transactionRepository.spendUtxo(utxo.address, utxo.txId.id, utxo.value)
                    }
                }
            }
            if (!commit) break
        }
        if (commit) {
            println("$ip - voting commit!")
            ZkRepository.vote2pc(request.id, ip, "commit")
        }

        commit = ZkRepository.poll2pc(request.id)
        if (commit) {
            println("committed!!!")
            for (tx in request.txListList) {
                if (ShardsRepository.getShardLeaderIp(tx.inputsList[0].address) == ip) {
                    executeTx(tx)
                    println("submitting tx for ${tx.inputsList[0].address} leader ${ShardsRepository.getShardLeaderIp(tx.inputsList[0].address)}")
                }
            }
        }
        else {
            for (tx in request.txListList) {
                if (ShardsRepository.getShardLeaderIp(tx.inputsList[0].address) == ip) {
                    for (utxo in tx.inputsList) {
                        BroadcastServiceImpl.send(msgType.ROLLBACK_UTXO, BroadcastServiceImpl.utxoToMsg(utxo))
                    }
                }
            }
        }
            /*for (tx in request.txListList) {
                if (ShardsRepository.getShardLeaderIp(tx.inputsList[0].address) == ip) {
                    transactionRepository.insertTx(tx)
                    for (utxo in tx.inputsList) {
                        transactionRepository.insertUtxo(utxo.txId.id, utxo.address, utxo.value)
                    }
                }
            }*/
        responseObserver.onNext(Empty.newBuilder().build())
        responseObserver.onCompleted()
    }

    override fun getLedger(request: Limit?, responseObserver: StreamObserver<LedgerTxEntryList>) {
        val ledgerTxEntryListBuilder : LedgerTxEntryList.Builder = LedgerTxEntryList.newBuilder()
        ledgerTxEntryListBuilder.addAllTxList(transactionRepository.getLedger(null))
        val response: LedgerTxEntryList = ledgerTxEntryListBuilder.build()
        responseObserver.onNext(response)
        responseObserver.onCompleted()
    }
}