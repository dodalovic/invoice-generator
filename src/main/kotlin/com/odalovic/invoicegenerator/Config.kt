package com.odalovic.invoicegenerator

import java.io.File

object Config {
    val CONFIG_DIR = "${System.getProperty("user.home")}/.invoice-generator"
    fun init() {
        val configDir = File(CONFIG_DIR)
        if (!configDir.exists()) {
            configDir.mkdirs()
            println("Config directory created: (${configDir.absolutePath})")
        }
        val meFileInConfigDir = File("${configDir.absolutePath}/me.yml")
        if (!meFileInConfigDir.exists()) {
            val templateContent = this::class.java.getResource("/me.yml").readText()
            meFileInConfigDir.writeText(templateContent)
            println("Created non-existing me template ${meFileInConfigDir.absolutePath}")
        }
        val clientFileInConfigDir = File("${configDir.absolutePath}/client.yml")
        if (!clientFileInConfigDir.exists()) {
            val templateContent = this::class.java.getResource("/client.yml").readText()
            clientFileInConfigDir.writeText(templateContent)
            println("Created non-existing client template ${clientFileInConfigDir.absolutePath}")
        }
        val translationsFileInConfigDir = File("${configDir.absolutePath}/translations.yml")
        if (!translationsFileInConfigDir.exists()) {
            val translationsContent = this::class.java.getResource("/translations.yml").readText()
            translationsFileInConfigDir.writeText(translationsContent)
            println("Created non-existing translations file ${translationsFileInConfigDir.absolutePath}")
        }
    }

    fun loadMeConfig() = File("${CONFIG_DIR}/me.yml").readText()
    fun loadClientConfig() = File("${CONFIG_DIR}/client.yml").readText()
    fun loadTranslationsConfig() = File("${CONFIG_DIR}/translations.yml").readText()
}
