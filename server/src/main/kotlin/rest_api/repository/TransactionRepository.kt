package com.example.api.repository

import com.example.api.repository.model.Transaction
import org.springframework.stereotype.Repository

@Repository
class TransactionRepository {
    var txMap: HashMap<Long, Transaction> = java.util.HashMap<Long, Transaction>()

    fun insertTx(tx: Transaction) {
        txMap[tx.id] = tx
    }

    fun deleteTx(txId: Long) {
        txMap.remove(txId)
    }

    fun exsitsTx(txId: Long): Boolean {
        return txMap.contains(txId)
    }

    fun getTx(txId: Long): Transaction? {
        return txMap[txId]
    }

    fun getTxMap(): ArrayList<Transaction>? {
        return ArrayList(txMap.values)
    }
}