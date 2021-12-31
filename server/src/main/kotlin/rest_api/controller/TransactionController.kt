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

    @GetMapping("/transaction")
    fun getTransactionLedger(): ArrayList<Transaction>? =
        TxClient.getTransactionLedger()

    @GetMapping("/transaction/{address}")
    fun getAllTransactions(@PathVariable("address") address: Address): ArrayList<Transaction>? =
        TxClient.getAllTransactions(address)

    @PostMapping("/transaction")
    fun submitTransaction(@RequestBody payload: Transaction): Unit =
        TxClient.submitTransaction(payload)

    @GetMapping("/utxo/{address}")
    fun getAllUtxos(@PathVariable("address") address: Address): ArrayList<UTxO>? =
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