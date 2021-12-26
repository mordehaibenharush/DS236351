package com.example.api.repository.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import java.io.Serializable
import javax.persistence.*

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class Transfer : Serializable{
    var address: String? = null
    var amount: Long? = null
}

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class UTxO : Serializable{
    var txId: Long? = null
    var address: String? = null
}

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class Transaction(var id: Long, var inputs: List<UTxO>, var outputs: List<Transfer>) {
}

/**
 * Represents the database entity for storing the employee details.
 *//*
@Entity
@Table(name = "transaction")
data class Transaction (
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,
    @Column(name = "inputs", nullable = false)
    val inputs: ArrayList<UTxO>,
    @Column(name = "outputs", nullable = false)
    val outputs: ArrayList<Transfer>,
)*/