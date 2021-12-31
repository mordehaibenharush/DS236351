package grpc_service

import com.example.api.exception.EmployeeNotFoundException
import com.example.api.repository.model.UTxO
import com.google.protobuf.Empty
import cs236351.txservice.*
import io.grpc.ManagedChannelBuilder
import org.springframework.http.HttpStatus
import java.net.InetAddress




object TxClient {
    @JvmStatic
    fun main(args: Array<String>) {
        val channel = ManagedChannelBuilder.forAddress("localhost", 8090)
            .usePlaintext()
            .build()
        val stub: TxServiceGrpc.TxServiceBlockingStub = TxServiceGrpc.newBlockingStub(channel)
        val txId : TxId = TxId.newBuilder().setId(1).build()
        val tx : Transaction = Transaction.newBuilder()
            .setTxId(txId)
            .addInputs(Utxo.newBuilder().setTxId(txId).setAddress("1"))
            .addOutputs(Transfer.newBuilder().setAddress("2").setAmount(3))
            .build()
        stub.insertTx(tx)
        val response : TransactionList = stub.getAllTx(Request.newBuilder().build())
        println(response)
        channel.shutdown()
    }

    private val stub: TxServiceGrpc.TxServiceBlockingStub

    init {
        val channel = ManagedChannelBuilder.forAddress("localhost", 8090)
            .usePlaintext()
            .build()
        this.stub = TxServiceGrpc.newBlockingStub(channel)
    }

    fun empty() : Empty {
        return Empty.newBuilder().build()
    }

    private fun request(address: Address) : Request {
        return Request.newBuilder().setAddress(address).build()
    }

    private fun trRequest(source: Address, txId: Id, tr: com.example.api.repository.model.Transfer) : TrRequest {
        return TrRequest.newBuilder().setSource(source).setTxId(txid(txId)).setTr(transfer(tr.address, tr.amount)).build()
    }

    private fun txid(id: Id) : TxId {
        return TxId.newBuilder().setId(id).build()
    }

    fun utxo(id: Id, address: Address, value: Value) : Utxo {
        val txId : TxId = TxId.newBuilder().setId(id).build()
        return Utxo.newBuilder().setTxId(txId).setAddress(address).setValue(value).build()
    }

    private fun transfer(address: Address, amount: Value) : Transfer {
        return Transfer.newBuilder().setAddress(address).setAmount(amount).build()
    }

    private fun transaction(id: Id, inputs: List<Utxo>, outputs: List<Transfer>) : Transaction {
        val txId : TxId = TxId.newBuilder().setId(id).build()
        return Transaction.newBuilder().setTxId(txId).addAllInputs(inputs).addAllOutputs(outputs).build()
    }

    private fun fromClientUtxo(utxo: Utxo) : UTxO {
        return UTxO(utxo.txId.id, utxo.address, utxo.value)
    }

    private fun toClientTransaction(tx: com.example.api.repository.model.Transaction) : Transaction {
        val inputs : List<Utxo> = tx.inputs.map { utxo(it.txId, it.address, it.value) }
        val outputs: List<Transfer> = tx.outputs.map { transfer(it.address, it.amount) }
        return this.transaction(tx.id, inputs, outputs)
    }

    private fun fromClientTransaction(tx: Transaction) : com.example.api.repository.model.Transaction {
        val inputs : List<UTxO> = tx.inputsList.map { UTxO(it.txId.id, it.address, it.value) }
        val outputs: List<com.example.api.repository.model.Transfer> = tx.outputsList.map { com.example.api.repository.model.Transfer(inputs[0].address, it.address, it.amount) }
        return com.example.api.repository.model.Transaction(tx.txId.id, inputs, outputs)
    }

    private fun insertTx(tx: com.example.api.repository.model.Transaction) {
        this.stub.insertTx(toClientTransaction(tx))
    }

    private fun deleteTx(txId: Id) {

    }

    private fun existsTx(txId: Id): Boolean {
        return this.stub.existsTx(txid(txId)).exists
    }

    private fun getTx(txId: Id): com.example.api.repository.model.Transaction {
        return fromClientTransaction(this.stub.getTx(txid(txId)))
    }

    private fun sendTr(txId: Id, tr: com.example.api.repository.model.Transfer) {
        /*val address = InetAddress.getLocalHost()
        val ip = address.hostAddress*/
        this.stub.sendTr(trRequest(tr.source, txId, tr))
    }

    private fun getAllUtxo(address: Address) : ArrayList<UTxO> {
        return ArrayList(this.stub.getAllUtxo(request(address)).utxoListList.map { fromClientUtxo(it) })
    }

    private fun getAllTx(address: Address): ArrayList<com.example.api.repository.model.Transaction> {
        return ArrayList(this.stub.getAllTx(request(address)).txListList.map { fromClientTransaction(it) })
    }

    private fun getLedger() : ArrayList<com.example.api.repository.model.Transaction> {
        return ArrayList(this.stub.getLedger(null).txListList.map { fromClientTransaction(it) })
    }

    /** ###################################### API FUNCTIONS ###################################### **/

    fun getAllUnspentTxOutput(address: Address): ArrayList<UTxO> = this.getAllUtxo(address)

    fun getAllTransactions(address: Address): ArrayList<com.example.api.repository.model.Transaction> = this.getAllTx(address)

    fun getTransactionById(id: Long): com.example.api.repository.model.Transaction = this.getTx(id)

    fun getTransactionLedger(): ArrayList<com.example.api.repository.model.Transaction> = this.getLedger()

    fun submitTransaction(transaction: com.example.api.repository.model.Transaction) {
        this.insertTx(transaction)
        for (tr in transaction.outputs) {
            this.sendTr(transaction.id, tr)
        }
    }

    fun submitTransfer(transfer: com.example.api.repository.model.Transfer) = this.sendTr(-1, transfer)

    fun updateTransactionById(txId: Long, transaction: com.example.api.repository.model.Transaction) {
        return if (this.existsTx(txId)) {
            this.insertTx(transaction)
        } else throw EmployeeNotFoundException(HttpStatus.NOT_FOUND, "No matching employee was found")
    }

    fun deleteTransactionById(txId: Long) {
        return if (this.existsTx(txId)) {
            this.deleteTx(txId)
        } else throw EmployeeNotFoundException(HttpStatus.NOT_FOUND, "No matching employee was found")
    }
}
