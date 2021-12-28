package grpc_service

import com.example.api.exception.EmployeeNotFoundException
import com.example.api.repository.model.UTxO
import com.google.protobuf.Empty
import cs236351.txservice.*
import io.grpc.ManagedChannelBuilder
import org.springframework.http.HttpStatus

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
        val response : TransactionList = stub.getAllTx(Empty.newBuilder().build())
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

    private fun empty() : Empty {
        return Empty.newBuilder().build()
    }

    private fun txid(id: Long) : TxId {
        return TxId.newBuilder().setId(id).build()
    }

    private fun utxo(id: Long, address: String) : Utxo {
        val txId : TxId = TxId.newBuilder().setId(id).build()
        return Utxo.newBuilder().setTxId(txId).setAddress(address).build()
    }

    private fun transfer(address: String, amount: Long) : Transfer {
        return Transfer.newBuilder().setAddress(address).setAmount(amount).build()
    }

    private fun transaction(id: Long, inputs: List<Utxo>, outputs: List<Transfer>) : Transaction {
        val txId : TxId = TxId.newBuilder().setId(id).build()
        return Transaction.newBuilder().setTxId(txId).addAllInputs(inputs).addAllOutputs(outputs).build()
    }

    private fun toClientTransaction(tx: com.example.api.repository.model.Transaction) : Transaction {
        val inputs : List<Utxo> = tx.inputs.map { this.utxo(it.txId, it.address) }
        val outputs: List<Transfer> = tx.outputs.map { this.transfer(it.address, it.amount) }
        return this.transaction(tx.id, inputs, outputs)
    }

    private fun fromClientTransaction(tx: Transaction) : com.example.api.repository.model.Transaction {
        val inputs : List<UTxO> = tx.inputsList.map { UTxO(it.txId.id, it.address) }
        val outputs: List<com.example.api.repository.model.Transfer> = tx.outputsList.map { com.example.api.repository.model.Transfer(it.address, it.amount) }
        return com.example.api.repository.model.Transaction(tx.txId.id, inputs, outputs)
    }

    private fun insertTx(tx: com.example.api.repository.model.Transaction) {
        this.stub.insertTx(toClientTransaction(tx))
    }

    private fun deleteTx(txId: Long) {

    }

    private fun existsTx(txId: Long): Boolean {
        return this.stub.existsTx(this.txid(txId)).exists
    }

    private fun getTx(txId: Long): com.example.api.repository.model.Transaction? {
        return fromClientTransaction(this.stub.getTx(this.txid(txId)))
    }

    private fun getTxMap(): ArrayList<com.example.api.repository.model.Transaction>? {

        return ArrayList(this.stub.getAllTx(this.empty()).txListList.map { fromClientTransaction(it) })
    }

    /**
     * Get all employees list.
     *
     * @return the list
     */
    fun getAllTransactions(): ArrayList<com.example.api.repository.model.Transaction>? = this.getTxMap()

    /**
     * Gets employees by id.
     *
     * @param employeeId the employee id
     * @return the employee by id
     * @throws EmployeeNotFoundException the employee not found exception
     */
    fun getTransactionById(id: Long): com.example.api.repository.model.Transaction? = this.getTx(id)

    /**
     * Create employee.
     *
     * @param employee the employee
     * @return the employee
     */
    fun createTransaction(transaction: com.example.api.repository.model.Transaction): Unit = this.insertTx(transaction)

    /**
     * Update employee.
     *
     * @param employeeId the employee id
     * @param employee the employee details
     * @return the employee
     * @throws EmployeeNotFoundException the employee not found exception
     */
    fun updateTransactionById(txId: Long, transaction: com.example.api.repository.model.Transaction) {
        return if (this.existsTx(txId)) {
            this.insertTx(transaction)
        } else throw EmployeeNotFoundException(HttpStatus.NOT_FOUND, "No matching employee was found")
    }

    /**
     * Delete employee.
     *
     * @param employeeId the employee id
     * @return the map
     * @throws EmployeeNotFoundException the employee not found exception
     */
    fun deleteTransactionById(txId: Long) {
        return if (this.existsTx(txId)) {
            this.deleteTx(txId)
        } else throw EmployeeNotFoundException(HttpStatus.NOT_FOUND, "No matching employee was found")
    }
}
