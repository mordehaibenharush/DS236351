package grpc_service

import cs236351.txservice.*
import zk_service.ZkRepository

//import zk_service.ZkRepository

typealias Id = Long
typealias Address = String
typealias Value = Long
typealias TimeStamp = Long

object TransactionRepository {
    private var zk: ZkRepository = ZkRepository
    var txMap: HashMap<Id, Transaction> = HashMap()
    var utxoMap: HashMap<Address, HashMap<Id, Utxo>> = HashMap()
    var txLedger: ArrayList<LedgerTxEntry> = ArrayList()

    init {
        utxoMap["0.0.0.0"] = hashMapOf(0.toLong() to TxClient.utxo(0, "0.0.0.0", Long.MAX_VALUE))
    }

    fun insertTx(tx: Transaction) {
        //ZkRepository.txLock()
        txMap[tx.txId.id] = tx
        txLedger.add(TxClient.ledgerTxEntry(tx.txId.id, tx))
        //ZkRepository.txUnlock()
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
        txLedger.sortBy { it.timestamp }
        return ArrayList((txLedger.filter { it.tx.inputsList[0].address == address }.map { it.tx }))
    }

    fun getLedger(limit : Limit?) : ArrayList<LedgerTxEntry> {
        txLedger.sortBy { it.timestamp }
        var ledger = txLedger
        if (limit != null)
            ledger = ArrayList(ledger.takeLast(limit.limit.toInt()))
        return ledger
    }

    fun existsUtxo(address: Address, txId: Id) : Boolean {
        if (utxoMap[address] == null)
            return false
        return utxoMap[address]!!.contains(txId)
    }

    private fun addUtxo(txId: Id, address: Address, value: Value) {
        var existingValue = 0.toLong()
        if (utxoMap[address] == null)
            utxoMap[address] = HashMap()
        if (existsUtxo(address, txId))
            existingValue = utxoMap[address]!![txId]!!.value
        utxoMap[address]!![txId] = TxClient.utxo(txId, address, existingValue+value)
    }

    fun insertUtxo(txId: Id, address: Address, value: Value) {
        //val mutex = zk.utxoLock(address)
        addUtxo(txId, address, value)
        //zk.utxoUnlock(mutex)
    }

    fun deleteUtxo(address : Address, utxo: Utxo) {
        utxoMap[utxo.address]?.remove(utxo.txId.id)
    }

    fun getUtxos(address: Address) : ArrayList<Utxo> {
        if (utxoMap[address] == null)
            return ArrayList()
        return ArrayList(utxoMap[address]!!.values)
    }

    fun getTotalUtxosValue(address: Address) : Long {
        val utxos = getUtxos(address)
        return utxos.fold(0.toLong()) {total, utxo -> total + utxo.value}
    }

    fun removeUtxoByValue(txId: Id, address : Address, amount : Value) : Boolean {
        var totalAmount : Value = 0
        val utxoKeysToRemove : ArrayList<Id> = ArrayList()
        //val mutex = zk.utxoLock(address)
        val utxos = utxoMap[address]?.filterValues { it.value == amount }?.toList()
        if (utxos != null && utxos.isNotEmpty()) {
                utxoMap[address]?.remove(utxos[0].first)
                //zk.utxoUnlock(mutex)
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
                addUtxo(txId, address, totalAmount - amount)
            }
            //zk.utxoUnlock(mutex)
            return true
        }
        //zk.utxoUnlock(mutex)
        return false
    }
}