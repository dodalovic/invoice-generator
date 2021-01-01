package com.odalovic.invoicegenerator

import be.quodlibet.boxable.BaseTable
import be.quodlibet.boxable.HorizontalAlignment
import be.quodlibet.boxable.VerticalAlignment
import be.quodlibet.boxable.line.LineStyle
import com.charleskorn.kaml.Yaml
import org.apache.fontbox.ttf.OTFParser
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.font.PDType0Font
import picocli.CommandLine
import java.awt.Color
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
    private lateinit var boldFont: PDType0Font
    private lateinit var regularFont: PDType0Font
    private val numberFormat = "#,##0.00"
    private val today = Date()

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

    private val pageTop = 730F

    override fun call(): Int {
        Config.init()
        me = Yaml.default.decodeFromString(Me.serializer(), Config.loadMeConfig())
        client = Yaml.default.decodeFromString(Client.serializer(), Config.loadClientConfig())
        translations = Yaml.default.decodeFromString(Translations.serializer(), Config.loadTranslationsConfig())
        for (lang in languages.split(",")) {
            renderPdf(lang)
        }
        return 0
    }

    private fun renderPdf(lang: String) {
        val document = PDDocument()
        initFonts(document)
        val firstPage = PDPage()
        val secondPage = PDPage()
        document.addPage(firstPage)
        document.addPage(secondPage)

        PDPageContentStream(document, firstPage).use {
            it.beginText()
            it.setFont(boldFont, 12F)
            it.newLineAtOffset(50F, pageTop)
            it.showText("${me.fullName} • ")
            it.setFont(regularFont, 11F)
            it.showText("${me.address.street} • ")
            val cityZip = "${me.address.zip} ${me.address.place}"
            it.showText(cityZip)
            it.endText()

            it.beginText()
            it.setLeading(20F)

            it.newLineAtOffset(50F, pageTop - 50F)

            it.showText(me.fullName)
            it.newLine()
            it.showText(me.address.street)
            it.newLine()
            it.showText(cityZip)
            it.newLine()
            val showCountry = lang["SHOW_COUNTRY"].toBoolean()
            if (showCountry) {
                it.showText(lang["COUNTRY"])
                it.newLine()
            }
            it.newLine()
            it.showTextBoldInline("${lang["TAX_NUMBER"]} ")
            it.showText(me.taxNumber)
            it.newLine()
            it.showTextBoldInline("${lang["VAT_ID"]} ")
            it.showText(me.vatID)
            it.endText()

            it.beginText()
            it.newLineAtOffset(425F, pageTop - 270F)
            it.showText("${me.address.place}, ${lang.longDate()}")
            it.endText()

            it.beginText()
            it.newLineAtOffset(50F, pageTop - 270F)
            it.setFont(boldFont, 18F)
            val prefixInvoice = SimpleDateFormat("yyyyMMdd").format(today)
            invoiceNumber = "$prefixInvoice-${client.invoiceId}"
            it.showText("${lang["INVOICE"]} #$invoiceNumber")
            it.endText()

            it.beginText()
            it.newLineAtOffset(50F, pageTop - 295F)
            it.showTextBoldInline("${lang["PERFORMANCE_PERIOD"]} ")
            it.showText(performancePeriod(lang))
            it.endText()

            val table = BaseTable(
                pageTop - 310, 0F, 0F, 510F, 50F, document, firstPage, true,
                true
            )
            val headerRow = table.createRow(20F)
            val descCell = headerRow.createCell(80F, lang["DESCRIPTION"])
            descCell.font = boldFont
            val pricesCell = headerRow.createCell(20F, lang["PRICE"])
            pricesCell.font = boldFont
            table.addHeaderRow(headerRow)
            for (item in client.items) {
                val row = table.createRow(10f)
                row.createCell(80F, item.description.getValue(lang))
                val parsed = englishNumberFormatter().parse(item.priceInEur)
                row.createCell(20F, numberFormatter(lang).format(parsed))
            }
            val emptyRow = table.createRow(20F)
            val emptyCell = emptyRow.createCell(100F, "")
            emptyCell.setLeftBorderStyle(LineStyle(Color.WHITE, 1F))
            emptyCell.setRightBorderStyle(LineStyle(Color.WHITE, 1F))
            val subtotalRow = table.createRow(10F)
            subtotalRow.createCell(
                80F,
                lang["SUBTOTAL"],
                HorizontalAlignment.RIGHT,
                VerticalAlignment.MIDDLE
            )
            val subtotal = client.items
                .sumByDouble { item -> englishNumberFormatter().parse(item.priceInEur).toDouble() }
            subtotalRow.createCell(20F, numberFormatter(lang).format(subtotal).toString())

            val vatRow = table.createRow(10F)
            vatRow.createCell(
                80F,
                "${lang["VAT"]} ${client.vatPercentage}%",
                HorizontalAlignment.RIGHT,
                VerticalAlignment.MIDDLE
            )

            val vatAmount = subtotal * client.vatPercentage / 100
            vatRow.createCell(20F, numberFormatter(lang).format(vatAmount))
            val totalRow = table.createRow(10F)

            val totalRowBackgroundColor = Color(235, 233, 228)
            val totalCell = totalRow.createCell(
                80F,
                lang["TOTAL"],
                HorizontalAlignment.RIGHT,
                VerticalAlignment.MIDDLE
            )
            totalCell.font = boldFont
            totalCell.fontSize = 13F
            totalCell.fillColor = totalRowBackgroundColor

            val formattedAmountToPay = numberFormatter(lang).format(subtotal + vatAmount)
            val totalAmountCell = totalRow.createCell(20F, formattedAmountToPay)
            totalAmountCell.font = boldFont
            totalAmountCell.fontSize = 13F
            totalAmountCell.fillColor = totalRowBackgroundColor

            table.draw()

            it.beginText()
            it.newLineAtOffset(50F, pageTop - 480F)
            it.setFont(regularFont, 11F)
            it.setLeading(20F)
            it.showText(lang["PLEASE_PAY"].format(formattedAmountToPay, client.daysToPay))
            it.newLine()
            it.newLine()
            it.showText(me.fullName)
            it.newLine()
            it.showTextBoldInline("${lang["BANK"]} ")
            it.showText(me.bank)
            it.newLine()
            it.showTextBoldInline("${lang["IBAN"]} ")
            it.showText(me.iban)
            it.newLine()
            it.showTextBoldInline("${lang["BIC"]} ")
            it.showText(me.bic)
            it.newLine()
            it.showTextBoldInline("${lang["PAYMENT_REASON"]} ")
            it.showText(invoiceNumber)
            it.newLine()
            it.newLine()
            it.showText(lang["THANK_YOU_NOTE"])
            it.newLine()
            it.newLine()
            it.showText(lang["KIND_REGARDS"])
            it.newLine()
            it.showText(me.fullName)
            it.endText()

        }
        PDPageContentStream(document, secondPage).use {
            it.beginText()
            it.setFont(boldFont, 12F)
            it.newLineAtOffset(50F, pageTop)
            it.showText("${me.fullName} • ")
            it.setFont(regularFont, 11F)
            it.showText("${me.address.street} • ")
            val cityZip = "${me.address.zip} ${me.address.place}"
            it.showText(cityZip)
            it.endText()

            it.beginText()
            it.setLeading(20F)

            it.newLineAtOffset(50F, pageTop - 50F)

            it.showTextBoldInline("Client invoice address:")
            it.newLine()
            it.showText(client.companyDetails.name)
            it.newLine()
            it.showText(client.companyDetails.invoiceAddress.line1)
            it.newLine()
            it.showText("${client.companyDetails.invoiceAddress.zip} ${client.companyDetails.invoiceAddress.place}")
            it.newLine()
            it.showTextBoldInline("VAT ID ")
            it.showText(client.companyDetails.vatId)
            it.endText()
        }
        document.use {
            val pdfFullPath = composeGeneratedPdfFullPath(lang)
            it.save(pdfFullPath).also { println("Successfully generated $pdfFullPath") }
        }
    }

    private fun composeGeneratedPdfFullPath(lang: String): String {
        val pdfName = desiredPdfNameWithoutExt?.plus("-$lang.pdf") ?: "$invoiceNumber-$lang.pdf"
        return "${Config.CONFIG_DIR}/$pdfName"
    }

    private fun performancePeriod(lang: String): String {
        val yearMonthObject = YearMonth.of(client.year, client.month)
        val daysInMonth = yearMonthObject.lengthOfMonth()
        val monthDisplayName = yearMonthObject.month.getDisplayName(TextStyle.FULL, Locale.forLanguageTag(lang))
        return "1 - $daysInMonth $monthDisplayName"
    }

    private fun numberFormatter(lang: String) =
        (NumberFormat.getNumberInstance(Locale.forLanguageTag(lang)) as DecimalFormat).apply { applyPattern(numberFormat) }

    private fun englishNumberFormatter() =
        (NumberFormat.getNumberInstance(Locale.forLanguageTag("EN")) as DecimalFormat).apply { applyPattern(numberFormat) }

    private fun initFonts(doc: PDDocument) {
        boldFont =
            PDType0Font.load(
                doc,
                OTFParser().parse(this::class.java.getResourceAsStream("/FiraSans-Bold.otf")),
                false
            )
        regularFont =
            PDType0Font.load(
                doc,
                OTFParser().parse(this::class.java.getResourceAsStream("/FiraSans-Book.otf")),
                false
            )
    }

    private fun String.longDate() =
        DateFormat.getDateInstance(DateFormat.LONG, Locale.forLanguageTag(this)).format(Date())

    private operator fun String.get(key: String): String {
        return translations.translations.getValue(this).getValue(key)
    }

    private fun PDPageContentStream.showTextBoldInline(text: String) {
        setFont(boldFont, 11F)
        showText(text)
        setFont(regularFont, 11F)
    }
}

fun main(args: Array<String>): Unit = exitProcess(CommandLine(InvoiceGenerator()).execute(*args))