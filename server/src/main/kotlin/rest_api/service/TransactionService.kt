package com.example.api.service

import com.example.api.exception.EmployeeNotFoundException
import com.example.api.repository.TransactionRepository
import com.example.api.repository.model.Transaction
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

/**
 * Service for interactions with employee domain object
 */
@Service
class TransactionService(private val transactionRepository: TransactionRepository) {

    /**
     * Get all employees list.
     *
     * @return the list
     */
    fun getAllTransactions(): ArrayList<Transaction>? = transactionRepository.getTxMap()

    /**
     * Gets employees by id.
     *
     * @param employeeId the employee id
     * @return the employee by id
     * @throws EmployeeNotFoundException the employee not found exception
     */
    fun getTransactionById(id: Long): Transaction? = transactionRepository.getTx(id)

    /**
     * Create employee.
     *
     * @param employee the employee
     * @return the employee
     */
    fun createTransaction(transaction: Transaction): Unit = transactionRepository.insertTx(transaction)

    /**
     * Update employee.
     *
     * @param employeeId the employee id
     * @param employee the employee details
     * @return the employee
     * @throws EmployeeNotFoundException the employee not found exception
     */
    fun updateTransactionById(txId: Long, transaction: Transaction) {
        return if (transactionRepository.exsitsTx(txId)) {
            transactionRepository.insertTx(transaction)
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
        return if (transactionRepository.exsitsTx(txId)) {
            transactionRepository.deleteTx(txId)
        } else throw EmployeeNotFoundException(HttpStatus.NOT_FOUND, "No matching employee was found")
    }
}