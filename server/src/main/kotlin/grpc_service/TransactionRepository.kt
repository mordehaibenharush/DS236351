package grpc_service

import cs236351.txservice.TxId
import cs236351.txservice.Utxo
import cs236351.txservice.Transfer
import cs236351.txservice.Transaction

class TransactionRepository {
    var txMap: HashMap<Long, Transaction> = java.util.HashMap<Long, Transaction>()

    fun insertTx(tx: Transaction) {
        txMap[tx.txId.id] = tx
    }

    fun deleteTx(txId: TxId) {
        txMap.remove(txId.id)
    }

    fun existsTx(txId: TxId): Boolean {
        return txMap.contains(txId.id)
    }

    fun getTx(txId: TxId): Transaction? {
        return txMap[txId.id]
    }

    fun getTxMap(): ArrayList<Transaction> {
        return ArrayList(txMap.values)
    }
}