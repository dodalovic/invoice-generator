package com.odalovic.invoicegenerator

import kotlinx.serialization.Serializable

@Serializable
data class Config(val invoiceId: String, val items: List<Item>, val personalData: PersonalData)

@Serializable
data class PersonalData(
    val fullName: String,
    val taxNumber: String,
    val vatID: String,
    val bank: String,
    val iban: String,
    val bic: String,
    val address: Address
) {
    @Serializable
    data class Address(val street: String, val zip: String, val place: String)
}

@Serializable
data class Item(val description: Map<String, String>, val priceInEur: String)

@Serializable
data class Translations(val translations: Map<String, Map<String, String>>)
