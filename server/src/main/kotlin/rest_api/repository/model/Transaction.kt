package com.example.api.repository.model

import com.fasterxml.jackson.annotation.JsonAutoDetect
import javax.persistence.*

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class Transfer {
    var address: String? = null
    var amount: Long? = null
}

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
class UTxO {
    var txId: Long? = null
    var address: String? = null
}

/**
 * Represents the database entity for storing the employee details.
 */
@Entity
@Table(name = "transaction")
data class Transaction(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long?,
    @Column(name = "inputs", nullable = false)
    val inputs: ArrayList<UTxO>,
    @Column(name = "outputs", nullable = false)
    val outputs: ArrayList<Transfer>,

    //val hi: Int
)