package com.example.api.controller

import com.example.api.repository.model.Transaction
import com.example.api.service.TransactionService
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

    @GetMapping("/transactions")
    fun getAllTransactions(): ArrayList<Transaction>? =
        TxClient.getAllTransactions()

    @GetMapping("/transactions/{id}")
    fun getTransactionById(@PathVariable("id") txId: Long): Transaction? =
        TxClient.getTransactionById(txId)

    @PostMapping("/transactions")
    fun createTransaction(@RequestBody payload: Transaction): Unit =
        TxClient.createTransaction(payload)

    @PutMapping("/transactions/{id}")
    fun updateTransactionById(@PathVariable("id") txId: Long, @RequestBody payload: Transaction): Unit =
        TxClient.updateTransactionById(txId, payload)

    @DeleteMapping("/transactions/{id}")
    fun deleteTransactionById(@PathVariable("id") txId: Long): Unit =
        TxClient.deleteTransactionById(txId)
}