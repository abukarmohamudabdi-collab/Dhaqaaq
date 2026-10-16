package so.dhaqaaq.app

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity() {

    private val INK = Color.parseColor("#0E2233")
    private val MUTED = Color.parseColor("#566B7C")
    private val ACCENT = Color.parseColor("#0A6C8F")
    private val SUN = Color.parseColor("#E9B43C")
    private val BG = Color.parseColor("#F4F7F9")
    private val LINE = Color.parseColor("#D3DEE6")
    private val OK = Color.parseColor("#1B7F4F")
    private val BAD = Color.parseColor("#B3361F")

    private lateinit var store: Store
    private lateinit var wallet: Wallet
    private val ui = Handler(Looper.getMainLooper())
    private var pending: (() -> Unit)? = null

    private lateinit var walletRow: LinearLayout
    private lateinit var toField: EditText
    private lateinit var amountField: EditText
    private lateinit var pinField: EditText
    private lateinit var outView: TextView
    private lateinit var historyBox: LinearLayout
    private var fallbackBtn: Button? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        wallet = store.lastWallet()
        window.statusBarColor = INK
        setContentView(buildUi())
        refreshWallets()
        refreshHistory()
    }

    // ---------- UI ----------
    private fun label(text: String): TextView = TextView(this).apply {
        this.text = text.uppercase(Locale.ROOT)
        textSize = 11f
        setTextColor(MUTED)
        typeface = Typeface.DEFAULT_BOLD
        letterSpacing = 0.08f
        setPadding(0, dp(14), 0, dp(6))
    }

    private fun field(hint: String, type: Int, maxLen: Int): EditText = EditText(this).apply {
        this.hint = hint
        inputType = type
        textSize = 17f
        setTextColor(INK)
        setHintTextColor(MUTED)
        filters = arrayOf(android.text.InputFilter.LengthFilter(maxLen))
        setPadding(dp(14), dp(12), dp(14), dp(12))
        background = GradientDrawable().apply {
            setColor(Color.WHITE); cornerRadius = dp(12).toFloat(); setStroke(dp(1), LINE)
        }
    }

    private fun button(text: String, primary: Boolean, onClick: () -> Unit): Button = Button(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) Color.WHITE else INK)
        background = GradientDrawable().apply {
            setColor(if (primary) ACCENT else Color.parseColor("#EAF0F4")); cornerRadius = dp(14).toFloat()
        }
        setOnClickListener { onClick() }
    }

    private fun buildUi(): View {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(BG) }

        // Cinwaan
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(INK)
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        bar.addView(TextView(this).apply {
            text = "Dhaqaaq"; textSize = 22f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        bar.addView(TextView(this).apply {
            text = "Dejinta"; textSize = 14f; setTextColor(SUN); typeface = Typeface.DEFAULT_BOLD
            setPadding(dp(10), dp(6), dp(10), dp(6))
            setOnClickListener { showSettings() }
        })
        root.addView(bar)

        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(4), dp(16), dp(24))
        }
        val scroll = ScrollView(this).apply { isFillViewport = true; addView(col) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))

        col.addView(label("Dooro boorsada"))
        walletRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        col.addView(walletRow)

        col.addView(label("Lambarka qofka (qaataha)"))
        toField = field("61 555 00 00", InputType.TYPE_CLASS_PHONE, 15)
        col.addView(toField)

        col.addView(label("Qaddarka (USD)"))
        amountField = field("0.00", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL, 8)
        col.addView(amountField)

        col.addView(label("PIN-kaaga"))
        pinField = field("••••", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD, 4)
        col.addView(pinField)
        col.addView(TextView(this).apply {
            text = "PIN-ka lama kaydiyo; waxaa loo isticmaalaa USSD-ka kaliya."
            textSize = 12f; setTextColor(MUTED); setPadding(0, dp(6), 0, 0)
        })

        val actions = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(16), 0, 0) }
        actions.addView(button("Dir lacag", true) { doSend() }, rowParams())
        actions.addView(button("Hubi haraaga", false) { doBalance() }, rowParams())
        actions.addView(button("Airtime / biil", false) { doAirtime() }, rowParams())
        actions.addView(button("Fur menu-ga boorsada", false) { doMenu() }, rowParams())
        col.addView(actions)

        col.addView(label("Jawaabta shirkadda"))
        outView = TextView(this).apply {
            text = "Halkan ayaa lagu soo bandhigayaa jawaabta shirkadda."
            textSize = 15f; setTextColor(MUTED)
            setPadding(dp(14), dp(14), dp(14), dp(14))
            background = GradientDrawable().apply {
                setColor(Color.WHITE); cornerRadius = dp(12).toFloat(); setStroke(dp(1), LINE)
            }
            setTextIsSelectable(true)
        }
        col.addView(outView)

        col.addView(label("Taariikh"))
        historyBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(historyBox)

        return root
    }

    private fun rowParams() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, dp(50)
    ).apply { bottomMargin = dp(8) }

    private fun refreshWallets() {
        walletRow.removeAllViews()
        WALLETS.forEach { w ->
            val selected = w.id == wallet.id
            val b = Button(this).apply {
                text = w.short
                isAllCaps = false
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (selected) Color.WHITE else INK)
                background = GradientDrawable().apply {
                    setColor(if (selected) ACCENT else Color.WHITE)
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), if (selected) ACCENT else LINE)
                }
                setPadding(dp(4), 0, dp(4), 0)
                setOnClickListener { wallet = w; store.setLastWallet(w); refreshWallets() }
            }
            walletRow.addView(b, LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(6) })
        }
    }

    private fun refreshHistory() {
        historyBox.removeAllViews()
        val h = store.history()
        if (h.isEmpty()) {
            historyBox.addView(TextView(this).apply { text = "Weli wax dhaqdhaqaaq ah ma jiro."; setTextColor(MUTED); textSize = 14f })
            return
        }
        h.forEach { line ->
            historyBox.addView(TextView(this).apply {
                text = line; textSize = 13.5f; setTextColor(INK)
                setPadding(0, dp(6), 0, dp(6))
            })
        }
        historyBox.addView(TextView(this).apply {
            text = "Tirtir taariikhda"; textSize = 13f; setTextColor(ACCENT); typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(10), 0, 0)
            setOnClickListener { store.clearHistory(); refreshHistory() }
        })
    }

    // ---------- Falgalka ----------
    private fun doSend() {
        val tpl = store.code(wallet, "send")
        if (tpl.isBlank()) return needCode("Dir lacag")
        val code = fill(tpl, needTo = true, needAmount = true) ?: return
        val to = toField.text.toString()
        run(code, "${wallet.name} · Dir ${amountField.text} → $to")
    }

    private fun doBalance() {
        val tpl = store.code(wallet, "balance")
        if (tpl.isBlank()) return needCode("Hubi haraaga")
        val code = fill(tpl, needTo = false, needAmount = false) ?: return
        run(code, "${wallet.name} · Hubin haraaga")
    }

    private fun doAirtime() {
        val tpl = store.code(wallet, "airtime")
        if (tpl.isBlank()) return needCode("Airtime / biil")
        val code = fill(tpl, needTo = true, needAmount = true) ?: return
        run(code, "${wallet.name} · Airtime ${amountField.text} → ${toField.text}")
    }

    private fun doMenu() {
        val menu = store.code(wallet, "menu")
        if (menu.isBlank()) return needCode("Menu-ga weyn")
        run(menu, "${wallet.name} · Menu")
    }

    /** Haddii koodh madhan yahay: u sheeg qofka, oo u furo menu-ga si gacanta loo isticmaalo. */
    private fun needCode(what: String) {
        val menu = store.code(wallet, "menu")
        val msg = "Koodhka \"$what\" ee ${wallet.name} weli lama dhigin. Ku dar Dejinta, ama isticmaal menu-ga gacanta."
        showOut(msg, MUTED)
        if (menu.isNotBlank()) showDialerButton(menu)
    }

    /** Buuxi template-ka; xaddid adag si xarfo gaar ah (* #) aysan koodhka u gelin. */
    private fun fill(tpl: String, needTo: Boolean, needAmount: Boolean): String? {
        var s = tpl
        if (s.contains("{to}") || needTo) {
            var d = toField.text.toString().filter { it.isDigit() }
            if (d.startsWith("252")) d = d.substring(3)
            if (d.length < 8 || d.length > 10) { toast("Lambarka qofka ma saxna."); return null }
            s = s.replace("{to}", d)
        }
        if (s.contains("{amount}") || needAmount) {
            val a = amountField.text.toString().replace(',', '.')
            if (!Regex("^\\d{1,6}(\\.\\d{1,2})?$").matches(a) || a.toDoubleOrNull() == null || a.toDouble() <= 0.0) {
                toast("Qaddarku ma saxna."); return null
            }
            s = s.replace("{amount}", a)
        }
        if (s.contains("{pin}")) {
            val p = pinField.text.toString()
            if (!Regex("^\\d{4}$").matches(p)) { toast("Geli PIN-kaaga 4-ka lambar ah."); return null }
            s = s.replace("{pin}", p)
        }
        if (!Regex("^[*#0-9.]+$").matches(s)) { toast("Koodhku wuxuu leeyahay xarfo aan loo oggolayn."); return null }
        return s
    }

    // ---------- USSD ----------
    private fun run(code: String, label: String) {
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            pending = { run(code, label) }
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 7)
            return
        }
        clearDialerButton()
        showOut("Waa la dirayaa… sug jawaabta shirkadda.", MUTED)
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        try {
            tm.sendUssdRequest(code, object : TelephonyManager.UssdResponseCallback() {
                override fun onReceiveUssdResponse(telephonyManager: TelephonyManager, request: String, response: CharSequence) {
                    showOut(response.toString(), INK)
                    addHistory("$label · waa la helay")
                }

                override fun onReceiveUssdResponseFailed(telephonyManager: TelephonyManager, request: String, failureCode: Int) {
                    showOut("USSD-ku wuu guuldareystay (koodh $failureCode). Hubi shabakada, SIM-ka iyo koodhka Dejinta.", BAD)
                    addHistory("$label · guuldarro")
                    val menu = store.code(wallet, "menu")
                    if (menu.isNotBlank()) showDialerButton(menu)
                }
            }, ui)
        } catch (e: SecurityException) {
            showOut("Ogolaansho ma helin. Ogolow 'Calls' Settings > Apps > Dhaqaaq.", BAD)
        } catch (e: Exception) {
            showOut("Khalad ayaa dhacay: ${e.message}", BAD)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 7) {
            val p = pending; pending = null
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) p?.invoke()
            else showOut("Ogolaanshaha wicitaanka ayaa loo baahan yahay si app-ku USSD u dirto.", BAD)
        }
    }

    /** Menu-ga (PIN la'aan) ayaa lagu furaa dialer-ka nidaamka: ammaan, PIN kuma dhaxayo taariikhda wicitaanada. */
    private fun showDialerButton(menuCode: String) {
        clearDialerButton()
        val b = button("Ku fur dialer-ka: $menuCode", false) {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(menuCode))))
        }
        fallbackBtn = b
        (outView.parent as LinearLayout).addView(b, (outView.parent as LinearLayout).indexOfChild(outView) + 1,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(46)).apply { topMargin = dp(8) })
    }

    private fun clearDialerButton() {
        fallbackBtn?.let { (it.parent as? ViewGroup)?.removeView(it) }
        fallbackBtn = null
    }

    private fun showOut(text: String, color: Int) { outView.text = text; outView.setTextColor(color) }
    private fun toast(t: String) = Toast.makeText(this, t, Toast.LENGTH_SHORT).show()

    private fun addHistory(line: String) {
        val t = SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date())
        store.addHistory("$t · $line")
        refreshHistory()
    }

    // ---------- Dejinta koodhadhka ----------
    private fun showSettings() {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(8), dp(20), dp(8)) }
        box.addView(TextView(this).apply {
            text = "Koodhadhka ${wallet.name}. Isticmaal {to}, {amount}, {pin}. Tusaale: *712*{to}*{amount}*{pin}#\nHubi koodhka shirkadda ka hor intaadan lacag badan dirin."
            textSize = 13f; setTextColor(MUTED)
        })
        val edits = HashMap<String, EditText>()
        CODE_KEYS.forEach { (key, title) ->
            box.addView(label(title))
            val e = field("", InputType.TYPE_CLASS_TEXT, 60).apply { setText(store.code(wallet, key)); textSize = 15f }
            edits[key] = e
            box.addView(e)
        }
        AlertDialog.Builder(this)
            .setTitle("Dejinta")
            .setView(ScrollView(this).apply { addView(box) })
            .setPositiveButton("Kaydi") { _, _ ->
                var bad = false
                edits.forEach { (k, e) ->
                    val v = e.text.toString().trim()
                    if (v.isNotEmpty() && !Regex("^[*#0-9.{}a-z]+$").matches(v)) bad = true
                    else store.setCode(wallet, k, v)
                }
                toast(if (bad) "Koodh qaar xarfo aan loo oggolayn ayuu leeyahay (lama kaydin)." else "Waa la kaydiyay.")
            }
            .setNegativeButton("Jooji", null)
            .show()
    }
}
