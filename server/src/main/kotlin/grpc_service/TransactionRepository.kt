package grpc_service

import cs236351.txservice.Limit
import cs236351.txservice.TxId
import cs236351.txservice.Utxo
import cs236351.txservice.Transaction
import zk_service.ZkRepository

typealias Id = Long
typealias Address = String
typealias Value = Long
typealias TimeStamp = Long

class TransactionRepository {
    private var zk: ZkRepository = ZkRepository()
    var txMap: HashMap<Id, Transaction> = java.util.HashMap<Id, Transaction>()
    var utxoMap: HashMap<Address, HashMap<Pair<Id, Value>, Utxo>> = HashMap<Address, HashMap<Pair<Id, Value>, Utxo>>()
    var txLedger: ArrayList<Pair<TimeStamp, Transaction>> = ArrayList<Pair<TimeStamp, Transaction>>()

    init {
        utxoMap["0.0.0.0"] = hashMapOf(Pair(0.toLong(), Long.MAX_VALUE) to TxClient.utxo(0, "0.0.0.0", Long.MAX_VALUE))
    }

    fun insertTx(tx: Transaction) {
        txMap[tx.txId.id] = tx
        txLedger.add(Pair(zk.getTimestamp(), tx))
    }

    fun deleteTx(txId: TxId) {
        txMap.remove(txId.id)
    }

    fun existsTx(txId: TxId) : Boolean {
        return txMap.contains(txId.id)
    }

    fun getTx(txId: TxId) : Transaction? {
        return txMap[txId.id]
    }

    fun getTxList(address: Address) : ArrayList<Transaction> {
        return ArrayList((txLedger
            .filter { it.second.inputsList[0].address == address }
            .map { (_, Tr) -> Tr }))
    }

    fun getLedger(limit : Limit?) : ArrayList<Transaction> {
        var ledger =  ArrayList(txLedger.map { (_, Tr) -> Tr })
        if (limit != null)
            ledger = ledger.takeLast(limit.limit.toInt()) as ArrayList<Transaction>
        return ledger
    }

    fun insertUtxo(utxo: Utxo) {
        if (utxoMap[utxo.address] == null)
            utxoMap[utxo.address] = HashMap()
        utxoMap[utxo.address]!![Pair(utxo.txId.id, utxo.value)] = utxo
    }

    fun deleteUtxo(address : Address, utxo: Utxo) {
        utxoMap[utxo.address]?.remove(Pair(utxo.txId.id, utxo.value))
    }

    fun getUtxos(address: Address) : ArrayList<Utxo> {
        if (utxoMap[address] == null)
            return ArrayList()
        return ArrayList(utxoMap[address]!!.values)
    }

    fun removeUtxoByValue(address : Address, amount : Value) : Boolean {
        var totalAmount : Value = 0
        val utxoKeysToRemove : ArrayList<Pair<Id, Value>> = ArrayList<Pair<Id, Value>>()
        val utxoToAdd : Utxo
        val utxos = utxoMap[address]?.filterValues { it.value == amount }?.toList()
        if (utxos != null && utxos.isNotEmpty()) {
                utxoMap[address]?.remove(utxos[0].first)
                return true
            }
        else {
            for (utxo in utxoMap[address].orEmpty().toList().sortedBy { (_, utxo) -> utxo.value }.toMap()) {
                utxoKeysToRemove.add(utxo.key)
                totalAmount += utxo.value.value

                if (totalAmount >= amount)
                    break
            }
        }

        if (totalAmount >= amount) {
            for (utxoKey in utxoKeysToRemove) {
                utxoMap[address]?.remove(utxoKey)
            }
            if (totalAmount > amount) {
                utxoToAdd = TxClient.utxo(-1, address, totalAmount - amount)
                utxoMap[address]!![Pair(utxoToAdd.txId.id, utxoToAdd.value)] = utxoToAdd
            }
            return true
        }
        return false
    }
}