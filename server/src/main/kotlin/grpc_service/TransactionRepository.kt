package grpc_service

import cs236351.txservice.*
//import zk_service.ZkRepository

typealias Id = Long
typealias Address = String
typealias Value = Long
typealias TimeStamp = Long

class TransactionRepository {
    //private var zk: ZkRepository = ZkRepository()
    var txMap: HashMap<Id, Transaction> = HashMap()
    var utxoMap: HashMap<Address, HashMap<Id, Utxo>> = HashMap()
    var txLedger: ArrayList<LedgerTxEntry> = ArrayList()

    init {
        utxoMap["0.0.0.0"] = hashMapOf(0.toLong() to TxClient.utxo(0, "0.0.0.0", Long.MAX_VALUE))
    }

    fun insertTx(tx: Transaction) {
        txMap[tx.txId.id] = tx
        txLedger.add(TxClient.ledgerTxEntry(/*zk.getTimestamp()*/-1, tx))
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
        return ArrayList((txLedger.filter { it.tx.inputsList[0].address == address }.map { it.tx }))
    }

    fun getLedger(limit : Limit?) : ArrayList<LedgerTxEntry> {
        var ledger =  txLedger
        if (limit != null)
            ledger = ledger.takeLast(limit.limit.toInt()) as ArrayList<LedgerTxEntry>
        return ledger
    }

    fun existsUtxo(address: Address, txId: Id) : Boolean {
        if (utxoMap[address] == null)
            return false
        return utxoMap[address]!!.contains(txId)
    }

    fun insertUtxo(txId: Id, address: Address, value: Value) {
        var existingValue = 0.toLong()
        if (utxoMap[address] == null)
            utxoMap[address] = HashMap()
        if (existsUtxo(address, txId))
            existingValue = utxoMap[address]!![txId]!!.value
        utxoMap[address]!![txId] = TxClient.utxo(txId, address, existingValue+value)
    }

    fun deleteUtxo(address : Address, utxo: Utxo) {
        utxoMap[utxo.address]?.remove(utxo.txId.id)
    }

    fun getUtxos(address: Address) : ArrayList<Utxo> {
        if (utxoMap[address] == null)
            return ArrayList()
        return ArrayList(utxoMap[address]!!.values)
    }

    fun removeUtxoByValue(address : Address, amount : Value) : Boolean {
        var totalAmount : Value = 0
        val utxoKeysToRemove : ArrayList<Id> = ArrayList<Id>()

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
                insertUtxo(-1, address, totalAmount - amount)
            }
            return true
        }
        return false
    }
}