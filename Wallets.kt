package so.dhaqaaq.app

import android.content.Context

/**
 * Boorsooyinka iyo koodhadhkooda USSD.
 *
 * Koodhadhku waa TEMPLATE: {to} = lambarka qaataha, {amount} = qaddarka, {pin} = PIN-ka.
 * Qiimayaasha hoose waa kuwa caadiga ah ee la og yahay; HA U BAAHAN YIHIIN INAAD HUBISO
 * (Dejinta > koodhadhka) ka hor intaadan lacag badan dirin, maxaa yeelay shirkadaha ayaa beddeli kara.
 * Haddii template madhan yahay, app-ku wuxuu furaa "menu" ka (tusaale *712#) si aad gacanta u isticmaasho.
 */
data class Wallet(val id: String, val name: String, val short: String, val defaults: Map<String, String>)

val WALLETS = listOf(
    Wallet(
        "evc", "EVC Plus", "EVC",
        mapOf("menu" to "*712#", "send" to "*712*{to}*{amount}*{pin}#", "balance" to "", "airtime" to "")
    ),
    Wallet(
        "edahab", "eDahab", "eDahab",
        mapOf("menu" to "*110#", "send" to "", "balance" to "", "airtime" to "")
    ),
    Wallet(
        "sahal", "Sahal", "Sahal",
        mapOf("menu" to "*883#", "send" to "", "balance" to "", "airtime" to "")
    ),
    Wallet(
        "premier", "Premier Wallet", "Premier",
        mapOf("menu" to "", "send" to "", "balance" to "", "airtime" to "")
    )
)

val CODE_KEYS = listOf(
    "menu" to "Menu-ga weyn (tusaale *712#)",
    "send" to "Dir lacag (isticmaal {to} {amount} {pin})",
    "balance" to "Hubi haraaga (isticmaal {pin} haddii loo baahdo)",
    "airtime" to "Airtime / biil (isticmaal {to} {amount} {pin})"
)

/** Kaydinta dejinta iyo taariikhda taleefankaaga gudihiisa. PIN lama kaydiyo weligiis. */
class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("dhaqaaq", Context.MODE_PRIVATE)

    fun code(w: Wallet, key: String): String = sp.getString("${w.id}.$key", null) ?: (w.defaults[key] ?: "")

    fun setCode(w: Wallet, key: String, value: String) {
        sp.edit().putString("${w.id}.$key", value.trim()).apply()
    }

    fun lastWallet(): Wallet = WALLETS.firstOrNull { it.id == sp.getString("last", "") } ?: WALLETS[0]
    fun setLastWallet(w: Wallet) { sp.edit().putString("last", w.id).apply() }

    fun history(): List<String> =
        (sp.getString("history", "") ?: "").split("\n").filter { it.isNotBlank() }

    fun addHistory(line: String) {
        val all = (listOf(line.replace("\n", " ")) + history()).take(30)
        sp.edit().putString("history", all.joinToString("\n")).apply()
    }

    fun clearHistory() { sp.edit().remove("history").apply() }
}
