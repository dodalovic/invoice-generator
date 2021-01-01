package com.odalovic.invoicegenerator

import kotlinx.serialization.Serializable

@Serializable
data class Me(
    val fullName: String,
    val taxNumber: String,
    val vatID: String,
    val bank: String,
    val iban: String,
    val bic: String,
    val address: Me.Address
) {
    @Serializable
    data class Address(val street: String, val zip: String, val place: String)
}

@Serializable
data class Client(
    val year: Int,
    val month: Int,
    val invoiceId: String,
    val vatPercentage: Byte,
    val daysToPay: Byte = 14,
    val items: List<Item>
) {
    @Serializable
    data class Item(val description: Map<String, String>, val priceInEur: String)
}

@Serializable
data class Translations(val translations: Map<String, Map<String, String>>)
