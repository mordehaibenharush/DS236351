package grpc_service

import cs236351.txservice.*
import zk_service.ZkRepository

//import zk_service.ZkRepository

typealias Id = Long
typealias Address = String
typealias Value = Long
typealias TimeStamp = Long
typealias Used = Boolean

object TransactionRepository {
    private var zk: ZkRepository = ZkRepository
    var txMap: HashMap<Id, Transaction> = HashMap()
    //var utxoMap: HashMap<Address, HashMap<Id, Utxo>> = HashMap()
    var txLedger: ArrayList<LedgerTxEntry> = ArrayList()
    var utxoMap: HashMap<Address, HashMap<Id, Pair<Utxo, Used>>> = HashMap()

    init {
        utxoMap["0.0.0.0"] = hashMapOf(0.toLong() to Pair(TxClient.utxo(0, "0.0.0.0", Long.MAX_VALUE), false))
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
/*        if (existsUtxo(address, txId))
            existingValue = utxosMap[address]!![txId]!!.first.value*/
        utxoMap[address]!![txId] = Pair(TxClient.utxo(txId, address, value), false)
    }

    fun spentUtxo(address : Address, txId: Id) =
        (existsUtxo(address, txId) && utxoMap[address]!![txId]!!.second)

    fun insertUtxo(txId: Id, address: Address, value: Value) {
        //val mutex = zk.utxoLock(address)
        if (!spentUtxo(address, txId))
            addUtxo(txId, address, value)
        else {
            val spent_val = getUtxo(address, txId).value
            if (value > spent_val)
            addUtxo(txId, address, value - spent_val)
        }
        //zk.utxoUnlock(mutex)
    }

    fun deleteUtxo(address : Address, utxo: Utxo) {
        utxoMap[utxo.address]?.remove(utxo.txId.id)
    }

    fun spendUtxo(address : Address, txId: Id, amount: Value) {
        if (!existsUtxo(address, txId))
            addUtxo(txId, address, amount)

        val utxo = getUtxo(address, txId)
        if (utxo.value > amount)
            addUtxo(txId, address, utxo.value - amount)
        else
            utxoMap[address]!![txId] = Pair(utxo, true)
    }

    fun getUtxo(address: Address, txId: Id) = utxoMap[address]!![txId]!!.first

    fun getUtxos(address: Address) : List<Utxo> {
        if (utxoMap[address] == null)
            return ArrayList()
        return ArrayList(utxoMap[address]!!.values.filter { !it.second }.map { it.first })
    }

    fun getTotalUtxosValue(address: Address) : Long {
        val utxos = getUtxos(address)
        return utxos.fold(0.toLong()) {total, utxo -> total + utxo.value}
    }

    fun removeUtxoByValue(txId: Id, address : Address, amount : Value) : Boolean {
        var totalAmount : Value = 0
        val utxoKeysToUse : ArrayList<Id> = ArrayList()
        //val mutex = zk.utxoLock(address)

        for (utxo in utxoMap[address].orEmpty().toList().filter { !it.second.second }.sortedByDescending { (_, pair) -> pair.first.value }.toMap()) {
            utxoKeysToUse.add(utxo.key)
            totalAmount += utxo.value.first.value

            if (totalAmount >= amount)
                break
        }

        if (totalAmount >= amount) {
            for (utxoKey in utxoKeysToUse) {
                spendUtxo(address, utxoKey, getUtxo(address, utxoKey).value)
            }
            if (totalAmount > amount) {
                addUtxo(utxoKeysToUse.last(), address, totalAmount - amount)
            }
            //zk.utxoUnlock(mutex)
            return true
        }
        //zk.utxoUnlock(mutex)
        return false
    }

    /*fun removeUtxoByValue(txId: Id, address : Address, amount : Value) : Boolean {
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
    }*/
}