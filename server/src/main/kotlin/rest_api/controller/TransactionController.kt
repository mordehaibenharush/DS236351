package com.example.api.controller

import com.example.api.repository.model.Transaction
import com.example.api.service.TransactionService
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
    fun getAllTransactions(): List<Transaction> = transactionService.getAllTransactions()

    @GetMapping("/transactions/{id}")
    fun getTransactionById(@PathVariable("id") txId: Long): Transaction =
        transactionService.getTransactionById(txId)

    @PostMapping("/transactions")
    fun createTransaction(@RequestBody payload: Transaction): Transaction = transactionService.createTransaction(payload)

    @PutMapping("/transactions/{id}")
    fun updateTransactionById(@PathVariable("id") txId: Long, @RequestBody payload: Transaction): Transaction =
        transactionService.updateTransactionById(txId, payload)

    @DeleteMapping("/transactions/{id}")
    fun deleteTransactionById(@PathVariable("id") txId: Long): Unit =
        transactionService.deleteTransactionById(txId)
}