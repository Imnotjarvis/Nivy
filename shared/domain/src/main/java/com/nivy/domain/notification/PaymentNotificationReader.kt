package com.nivy.domain.notification

/** A notification as an app posted it: the app, its name, and the notification's title and text. */
data class NotificationText(
    val packageName: String,
    val appName: String?,
    val title: String?,
    val text: String?,
)

/** Which way money went. */
enum class MoneyWay { Spent, Received, Sent, Cash }

/** What a notification says about money. */
sealed interface Reading {
    /**
     * Money that moved: [cents] of [currency], an ISO code or the symbol shown when it's ambiguous,
     * like "$"; [way] is null when nothing says which way it went. [merchant] is the shop or the
     * person, [card] the card's last four digits.
     */
    data class Money(
        val way: MoneyWay?,
        val cents: Long,
        val currency: String,
        val merchant: String?,
        val card: String?,
    ) : Reading

    data class Ignored(val why: Why) : Reading
}

/**
 * One reader for every app's payment notifications, in Italian and English: the amount next to
 * its currency, which way the money went, the shop or the person, and the card's last digits; or
 * why the notification isn't a payment. Codes, holds, declines, reminders, offers and access
 * alerts carry amounts too, so they're told apart first, by their words. Only Google Wallet needs
 * a rule of its own: its payments have no verb.
 */
object PaymentNotificationReader {
    /** Google Wallet: its payments name the shop in the title and have no verb. */
    const val GoogleWallet = "com.google.android.apps.walletnfcrel"

    fun read(notification: NotificationText): Reading {
        val title = notification.title?.let(::normalize).orEmpty()
        val all = listOf(title, notification.text?.let(::normalize).orEmpty())
            .filter { it.isNotEmpty() }
            .joinToString("\n")
        val notMoney = notMoney(all)
        val amount = amountIn(all)
        return when {
            notMoney != null -> Reading.Ignored(notMoney)
            amount == null -> Reading.Ignored(Why.NoAmount)
            else -> money(notification, title, all, amount)
        }
    }

    /** No-break and thin spaces as spaces, a line's runs of spaces as one, round dots as Wallet's. */
    fun normalize(text: String): String = text
        .replace(SpaceLike, " ")
        .replace('●', '•')
        .lines()
        .joinToString("\n") { it.replace(Spaces, " ").trim() }
        .trim()

    private fun money(notification: NotificationText, title: String, all: String, amount: Amount): Reading.Money {
        val card = CardPatterns.firstNotNullOfOrNull { it.find(all)?.groupValues?.get(1) }
        val way = WaySigns.entries.firstOrNull { it.value.containsMatchIn(all) }?.key
            ?: MoneyWay.Spent.takeIf { notification.packageName == GoogleWallet && card != null }
        val merchant = merchantAfter(all, amount, way) ?: titleMerchant(title, notification.appName)
        return Reading.Money(
            way = way,
            cents = amount.cents,
            currency = amount.currency,
            merchant = merchant,
            card = card,
        )
    }

    private fun notMoney(all: String): Why? =
        if (StrongCode.containsMatchIn(all) || CodeWord.containsMatchIn(all) && CodeToken.containsMatchIn(all)) {
            Why.Code
        } else {
            NotMoneySigns.entries.firstOrNull { it.value.containsMatchIn(all) }?.key
        }

    // ---- The amount

    private class Amount(val cents: Long, val currency: String, val end: Int)

    // The first amount that isn't a balance.
    private fun amountIn(all: String): Amount? = AmountPattern.findAll(all)
        .firstOrNull { match ->
            val before = all.substring(maxOf(0, match.range.first - BalanceLookBehind), match.range.first)
            !Balance.containsMatchIn(before)
        }
        ?.let { match ->
            val groups = match.groupValues
            val sign = listOf(SymbolBefore, CodeBefore, SymbolAfter, CodeAfter)
                .map { groups[it] }
                .first { it.isNotEmpty() }
            val number = groups[NumberAfterCurrency].ifEmpty { groups[NumberBeforeCurrency] }
            Amount(cents(number), currencyCode(sign), match.range.last + 1)
        }

    // In its currency's cents: the last separator is the decimal mark when one or two digits
    // follow it; the others group thousands.
    private fun cents(number: String): Long {
        val decimals = Decimals.find(number)
        val whole = (decimals?.let { number.substring(0, it.range.first) } ?: number).filter(Char::isDigit)
        return (whole + (decimals?.groupValues?.get(1)?.padEnd(2, '0') ?: "00")).toLong()
    }

    private fun currencyCode(sign: String): String = when (sign) {
        "€", "euro", "Euro", "Eur" -> "EUR"
        "US$" -> "USD"
        "£" -> "GBP"
        else -> sign
    }

    // ---- The shop or the person

    // After a word that names them, past the amount, else anywhere; else after a dash, or a comma
    // and a capital, right after the amount.
    private fun merchantAfter(all: String, amount: Amount, way: MoneyWay?): String? {
        val leadIn = LeadIns.getValue(way)
        val start = (leadIn.find(all, amount.end) ?: leadIn.find(all) ?: AfterAmount.matchAt(all, amount.end))
            ?.range?.last?.plus(1)
        return start?.let { merchantFrom(all.substring(it)) }
    }

    // What follows, up to the next sentence, date, time, card, second amount or balance.
    private fun merchantFrom(rest: String): String? {
        val stop = listOfNotNull(MerchantStop.find(rest)?.range?.first, AmountPattern.find(rest)?.range?.first)
            .minOrNull() ?: rest.length
        return clean(rest.substring(0, stop))
    }

    // The title names the shop, unless it's the app's own name or a heading.
    private fun titleMerchant(title: String, appName: String?): String? {
        val appsOwn = appName != null && title.contains(appName, ignoreCase = true)
        val heading = AmountPattern.containsMatchIn(title) ||
            WaySigns.values.any { it.containsMatchIn(title) } ||
            Heading.containsMatchIn(title)
        return if (appsOwn || heading) null else clean(title)
    }

    // Without emoji and symbols, trailing punctuation or runs of spaces; null when no word is left.
    private fun clean(text: String): String? = text
        .filterNot { it.isSurrogate() || it == VariationSelector || Character.getType(it) == SymbolType }
        .replace(Spaces, " ")
        .trim()
        .trimEnd('.', ',', ';', ':', '-', '–')
        .trim()
        .takeIf { it.length >= MerchantMinLength && it.any(Char::isLetter) }
        ?.take(MerchantMaxLength)

    // ---- Patterns

    private val SpaceLike = Regex("[    ]")
    private val Spaces = Regex("[ \t]+")
    private const val VariationSelector = '️'
    private val SymbolType = Character.OTHER_SYMBOL.toInt()
    private const val MerchantMinLength = 2
    private const val MerchantMaxLength = 60

    // How far before an amount a balance word makes it a balance.
    private const val BalanceLookBehind = 25

    private const val Number = """(?<![\d.,])\d+(?:[.,]\d{3})*(?:[.,]\d{1,2})?(?!\d)"""
    private const val Symbol = """€|US\$|\$|£|¥"""
    private val Codes = listOf(
        "EUR", "USD", "GBP", "CHF", "JPY", "CNY", "SEK", "NOK", "DKK", "PLN", "CZK", "HUF", "RON",
        "BGN", "TRY", "AUD", "CAD", "NZD", "MXN", "BRL", "ARS", "COP", "CLP", "PEN", "INR", "AED",
        "SAR", "ZAR", "THB", "SGD", "HKD", "KRW", "ILS", "EGP", "MAD", "RUB", "UAH", "ISK",
        "[Ee]uro", "Eur",
    ).joinToString("|")

    // A number with its sign before (groups 1 and 2, the number in 3) or after it (the number in
    // 4, the sign in 5 and 6). Codes are in capitals, as apps write them.
    // AmountPattern's groups: a currency, as a symbol or a code, then the number; or the number,
    // then its currency as a symbol or a code.
    private const val SymbolBefore = 1
    private const val CodeBefore = 2
    private const val NumberAfterCurrency = 3
    private const val NumberBeforeCurrency = 4
    private const val SymbolAfter = 5
    private const val CodeAfter = 6
    private val AmountPattern = Regex(
        """(?:($Symbol)|\b($Codes)(?![A-Za-z]))\s?($Number)|($Number)\s?(?:($Symbol)|($Codes)\b)""",
    )
    private val Decimals = Regex("""[.,](\d{1,2})$""")
    private val Balance = Regex(
        """saldo|disponibil|disposizione|balance|available|plafond|residu""",
        RegexOption.IGNORE_CASE,
    )

    private val CardPatterns = listOf(
        Regex("""(?:[•*]{1,4}|[Xx]{2,4})\s?(\d{4})\b"""),
        Regex("""\b(?:carta|card)\s+[•*]*\s?(\d{4})\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:termina(?:nte)?|ending)(?:\s+(?:con|in|with))?\s+(\d{4})\b""", RegexOption.IGNORE_CASE),
    )

    private fun signs(vararg words: String) = Regex("""\b(?:${words.joinToString("|")})\b""", RegexOption.IGNORE_CASE)

    private val StrongCode = signs(
        "da autorizzare", "autorizza (?:il|la|l'|questo|questa)", "approva", "conferma (?:il|la|l'|questo|questa)",
        "3d ?secure", "3ds", "securecode", "safekey", "one-?time", "password monouso", "codice monouso",
    )
    private val CodeWord = signs(
        "codice", "code", "otp", "pin", "password", "passcode", "verifica", "verify", "verification",
    )

    // A code: four to eight digits, not a card's, a time, a date or an amount.
    private val CodeToken = Regex("""(?<![\d•*xX.,:/])\d{4,8}(?![\d.,:/])""")

    private val NotMoneySigns = linkedMapOf(
        Why.Hold to signs(
            "blocco temporaneo", "importo bloccato", "bloccat[oi] temporaneamente", "pre-?autorizzazione",
            "pagamento aggiornato", "temporary hold", "on hold", "pre-?authori[sz]ation", "payment updated",
            "authori[sz]ation hold",
        ),
        Why.Declined to signs(
            "rifiutat[oa]", "non riuscit[oa]", "non (?:è |e )?andat[oa] a buon fine", "negat[oa]", "respint[oa]",
            "annullat[oa]", "stornat[oa]", "revocat[oa]", "non autorizzat[oa]", "declined", "failed",
            "cancell?ed", "reversed", "rejected", "unsuccessful",
        ),
        Why.LowBalance to signs(
            "saldo (?:inferiore|basso|insufficiente)", "fondi insufficienti", "disponibilit[àa] insufficiente",
            "low balance", "insufficient funds", "balance is low",
        ),
        Why.NotYet to signs(
            "verr[àa] addebitat[oa]", "sar[àa] addebitat[oa]", "verranno addebitat[ie]", "in scadenza", "scade",
            "scadr[àa]", "promemoria", "ricorda", "ti ricordiamo", "in sospeso", "in attesa",
            "will be (?:debited|charged)", "payment due", "is due", "due date", "upcoming", "reminder", "pending",
            "scheduled",
        ),
        Why.Offer to signs(
            "termini e condizioni", "si applicano", "offerta", "promozione", "promo", "gratis",
            "terms and conditions", "terms apply", "promotion", "offer",
        ),
        Why.Access to signs(
            "accesso", "login", "log-in", "nuovo dispositivo", "new device", "sign-?in", "signed in",
        ),
    )

    // Checked in this order: money in, money sent, cash, then a payment.
    private val WaySigns = linkedMapOf(
        MoneyWay.Received to signs(
            "ricevut\\w*", "accredit\\w*", "rimbors\\w*", "cashback", "received", "refund\\w*", "credited",
            "incoming",
        ),
        MoneyWay.Sent to signs(
            "inviat\\w*", "bonific\\w*", "giroconto", "trasferiment\\w*", "sent", "transfer\\w*", "outgoing",
        ),
        MoneyWay.Cash to signs("preliev\\w*", "prelevat\\w*", "withdrawal", "withdrew"),
        MoneyWay.Spent to signs(
            "pagat\\w*", "spes[oa]", "pagament\\w*", "acquist\\w*", "addebit\\w*", "operazion\\w*",
            "autorizzat\\w*", "transazion\\w*", "paid", "spent", "payment", "purchase\\w*", "charged", "debited",
            "transaction",
        ),
    )

    // The words before a shop, or a person, by which way the money went.
    private val LeadIns: Map<MoneyWay?, Regex> = mapOf(
        MoneyWay.Spent to leadIns("presso", "da", "at", "a", "su", "c/o", "@", "to"),
        MoneyWay.Received to leadIns("da", "from", "by"),
        MoneyWay.Sent to leadIns("a", "to"),
        MoneyWay.Cash to leadIns("presso", "at", "da"),
        null to leadIns("presso", "da", "at"),
    )

    private fun leadIns(vararg words: String) = Regex(
        """(?<!\w)(?:${words.joinToString("|") { Regex.escape(it) }})\s+(?=\S)""",
        RegexOption.IGNORE_CASE,
    )

    // A dash, or a comma and a capital, right after the amount.
    private val AfterAmount = Regex("""\s*(?:[-–]\s+|,\s+(?=\p{Lu}))""")

    private val MerchantStop = Regex(
        """[,;](?:\s|$)|\.(?:\s|$)|\n|\s[-–·|]\s|""" +
            """\s(?:il|alle|ore|on|con|with|per|tramite|via|in data)\s|\s(?:saldo|balance)\b""",
        RegexOption.IGNORE_CASE,
    )

    private val Heading = signs(
        "notifica", "avviso", "movimento", "alert", "notification", "carta", "card", "conto", "account",
    )
}
