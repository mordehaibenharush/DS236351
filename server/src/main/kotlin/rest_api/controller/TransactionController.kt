package com.example.api.controller

import com.example.api.repository.model.Transaction
import com.example.api.repository.model.Transfer
import com.example.api.repository.model.UTxO
import com.example.api.service.TransactionService
import grpc_service.Address
import grpc_service.Id
import grpc_service.TxClient
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

/**
 * Controller for REST API endpoints
 */
@RestController
class TransactionController(private val transactionService: TransactionService) {

    @GetMapping("/ledger")
    fun getTransactionLedger(): List<Transaction>? =
        TxClient.getTransactionLedger(null)

    @GetMapping("/ledger/{limit}")
    fun getLimitedTransactionLedger(@PathVariable("limit") limit: Int): List<Transaction>? =
        TxClient.getTransactionLedger(limit)

    @GetMapping("/transaction/{address}")
    fun getAllTransactions(@PathVariable("address") address: Address): List<Transaction>? =
        TxClient.getAllTransactions(address, null)

    @GetMapping("/transaction/{address}/{limit}")
    fun getLimitedAllTransactions(@PathVariable("address") address: Address, @PathVariable("limit") limit: Int): List<Transaction>? =
        TxClient.getAllTransactions(address, limit)

    @PostMapping("/transaction")
    fun submitTransaction(@RequestBody payload: Transaction): Unit =
        TxClient.submitTransaction(payload)

    @PostMapping("/transactions")
    fun submitTransactionList(@RequestBody payload: List<Transaction>): Unit =
        TxClient.submitTransactionList(payload)

    @GetMapping("/utxo/{address}")
    fun getAllUtxos(@PathVariable("address") address: Address): List<UTxO>? =
        TxClient.getAllUnspentTxOutput(address)

    @PostMapping("/transfer")
    fun submitTransfer(@RequestBody payload: Transfer): Unit =
        TxClient.submitTransfer(payload)

    /*@GetMapping("/transactions/{id}")
    fun getTransactionById(@PathVariable("id") txId: Id): Transaction? =
        TxClient.getTransactionById(txId)*/

    /*@PutMapping("/transactions/{id}")
    fun updateTransactionById(@PathVariable("id") txId: Id, @RequestBody payload: Transaction): Unit =
        TxClient.updateTransactionById(txId, payload)*/

    /*@DeleteMapping("/transactions/{id}")
    fun deleteTransactionById(@PathVariable("id") txId: Id): Unit =
        TxClient.deleteTransactionById(txId)*/
}