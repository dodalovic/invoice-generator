package com.odalovic.invoicegenerator

import com.charleskorn.kaml.Yaml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.asciidoctor.Asciidoctor.Factory.create
import org.asciidoctor.OptionsBuilder.options
import org.asciidoctor.SafeMode
import picocli.CommandLine
import java.io.File
import java.text.DateFormat
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*
import java.util.concurrent.Callable
import kotlin.system.exitProcess


@CommandLine.Command(
    name = "generate-invoice",
    mixinStandardHelpOptions = true,
    version = ["generate-invoice 1.0"],
    description = ["Generates invoice for particular client"]
)
class InvoiceGenerator : Callable<Int> {
    private val numberFormat = "#,##0.00"

    @CommandLine.Option(
        names = ["-l", "--languages"],
        description = ["Comma delimited list of languages to render PDF in, e.g -l EN,DE or -l EN"]
    )
    var languages: String = "EN"

    private lateinit var invoiceNumber: String

    private lateinit var me: Me
    private lateinit var client: Client
    private lateinit var translations: Translations

    @CommandLine.Option(
        names = ["--pdf-name", "-p"],
        description = ["Name (without .pdf) of file(s) to be generated"]
    )
    private var desiredPdfNameWithoutExt: String? = null

    override fun call(): Int {
        Config.init()
        me = Yaml.default.decodeFromString(Me.serializer(), Config.loadMeConfig())
        client = Yaml.default.decodeFromString(Client.serializer(), Config.loadClientConfig())
        translations = Yaml.default.decodeFromString(Translations.serializer(), Config.loadTranslationsConfig())
        runBlocking {
            for (lang in languages.split(",")) {
                launch(Dispatchers.Default) {
                    renderPdf(lang)
                }
            }
        }
        return 0
    }

    private fun renderPdf(lang: String) {
        val asciidoctor = create()
        val prefixInvoice = SimpleDateFormat("yyyyMMdd").format(Date())
        invoiceNumber = "$prefixInvoice-${client.invoiceId}"

        val generatedPdfFullPath = composeGeneratedPdfFullPath(lang)
        val options = options()
            .inPlace(true)
            .toFile(File(generatedPdfFullPath))
            .safe(SafeMode.UNSAFE)
            .backend("pdf")
            .asMap()

        val subtotal = client.items
            .sumByDouble { item -> englishNumberFormatter().parse(item.priceInEur).toDouble() }
        val vatAmount = subtotal * client.vatPercentage / 100
        val formattedAmountToPay = numberFormatter(lang).format(subtotal + vatAmount)

        asciidoctor.convert(
            """
            :nofooter:
            
            pass:a,q[*${me.fullName}*] • ${me.address.street} • ${me.address.zip} ${me.address.place}
            
            :hardbreaks:
             
            
            ${me.fullName}
            ${me.address.street}
            ${me.address.zip} ${me.address.place}
            pass:a,q[*${lang["TAX_NUMBER"]}*] ${me.taxNumber}
            pass:a,q[*${lang["VAT_ID"]}*] ${me.vatID}
            
            :hardbreaks:
             
             
            
            [cols="<,>",frame=none,grid=none]
            |===
            |*${lang["INVOICE"]}* $invoiceNumber | ${me.address.place}, ${lang.longDate()}
            |===
            
            *${lang["PERFORMANCE_PERIOD"]}* ${performancePeriod(lang)}
            
            [cols="70,30",subs="attributes+,verbatim",options=header]
            |===
            | ${lang["DESCRIPTION"]} | ${lang["PRICE"]}
            ${
                client.items.joinToString(transform = { item ->
                    "|${item.description[lang]} | ${
                        numberFormatter(lang).format(
                            englishNumberFormatter().parse(item.priceInEur)
                        )
                    }"
                })
            }
            | {nbsp} | {nbsp}
            >| *${lang["SUBTOTAL"]}* | ${numberFormatter(lang).format(subtotal)}
            >| *${lang["VAT"]} ${client.vatPercentage}%* | ${numberFormatter(lang).format(vatAmount)}
            >| *${lang["TOTAL"]}* | $formattedAmountToPay
            |===
            
            
            ${lang["PLEASE_PAY"].format(formattedAmountToPay, client.daysToPay)}
            
            :hardbreaks:
             
            
            ${me.fullName}
            
            *${lang["BANK"]}* ${me.bank}
            
            *${lang["IBAN"]}* ${me.iban}
            
            *${lang["BIC"]}* ${me.bic}
            
            *${lang["PAYMENT_REASON"]}* $invoiceNumber
            
            :hardbreaks:
             
             
            
            ${lang["THANK_YOU_NOTE"]}
            
            :hardbreaks:
             
            
            ${lang["KIND_REGARDS"]}
            ${me.fullName}
            
            <<< 
            
            pass:a,q[*${me.fullName}*] • ${me.address.street} • ${me.address.zip} ${me.address.place}
            
            :hardbreaks:
             

            *${lang["CLIENT_INVOICE_ADDRESS"]}*
            
            ${client.companyDetails.name}
            
            ${client.companyDetails.invoiceAddress.line1}
            
            ${client.companyDetails.invoiceAddress.zip} ${client.companyDetails.invoiceAddress.place}
            
            *${lang["VAT_ID"]}* ${client.companyDetails.vatId}
            
        """.trimIndent(), options
        ).also { println("Generated invoice $generatedPdfFullPath") }
    }

    private fun composeGeneratedPdfFullPath(lang: String): String {
        val pdfName = desiredPdfNameWithoutExt?.plus("-$lang.pdf") ?: "$invoiceNumber-$lang.pdf"
        return "${Config.CONFIG_DIR}/$pdfName"
    }

    private fun performancePeriod(lang: String): String {
        val yearMonthObject = YearMonth.of(client.year, client.month)
        val daysInMonth = yearMonthObject.lengthOfMonth()
        val monthDisplayName = yearMonthObject.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag(lang))
        val yearDisplayName = yearMonthObject.year
        return "1 - $daysInMonth $monthDisplayName $yearDisplayName"
    }

    private fun numberFormatter(lang: String) =
        (NumberFormat.getNumberInstance(Locale.forLanguageTag(lang)) as DecimalFormat).apply { applyPattern(numberFormat) }

    private fun englishNumberFormatter() =
        (NumberFormat.getNumberInstance(Locale.forLanguageTag("EN")) as DecimalFormat).apply { applyPattern(numberFormat) }

    private fun String.longDate() =
        DateFormat.getDateInstance(DateFormat.LONG, Locale.forLanguageTag(this)).format(Date())

    private operator fun String.get(key: String): String {
        return translations.translations.getValue(this).getValue(key)
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(InvoiceGenerator()).execute(*args))