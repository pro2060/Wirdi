package com.proart.wirdi

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class ReaderActivity : AppCompatActivity() {
    private var session = -1
    private var first = 1
    private var idx = 0
    private var token = 0

    private lateinit var titleTv: TextView
    private lateinit var msgTv: TextView
    private lateinit var iv: ImageView
    private lateinit var prevBtn: MaterialButton
    private lateinit var nextBtn: MaterialButton
    private lateinit var doneBtn: MaterialButton
    private lateinit var settingsBtn: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = intent.getIntExtra("session", -1)
        first = intent.getIntExtra("first", 1)

        val main = LinearLayout(this)
        main.orientation = LinearLayout.VERTICAL
        main.setBackgroundColor(Ui.BG)

        titleTv = Ui.tv(this, "", 16f, true, Ui.GREEN, 8)
        titleTv.gravity = Gravity.CENTER
        titleTv.setPadding(0, Ui.dp(this, 12), 0, 0)
        main.addView(titleTv)

        val sv = ScrollView(this)
        sv.layoutParams = LinearLayout.LayoutParams(Ui.MATCH, 0, 1f)
        val inner = LinearLayout(this)
        inner.orientation = LinearLayout.VERTICAL
        msgTv = Ui.tv(this, "", 14f, false, Ui.MUTED, 8)
        msgTv.gravity = Gravity.CENTER
        iv = ImageView(this)
        iv.adjustViewBounds = true
        iv.layoutParams = LinearLayout.LayoutParams(Ui.MATCH, Ui.WRAP)
        inner.addView(msgTv)
        inner.addView(iv)
        sv.addView(inner)
        main.addView(sv)

        val bar = LinearLayout(this)
        bar.orientation = LinearLayout.VERTICAL
        bar.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 4))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        prevBtn = Ui.btn(this, "السابقة", false) { idx = 0; show() }
        nextBtn = Ui.btn(this, "التالية", false) { idx = 1; show() }
        val lp1 = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        lp1.marginEnd = Ui.dp(this, 8)
        prevBtn.layoutParams = lp1
        nextBtn.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        row.addView(prevBtn)
        row.addView(nextBtn)
        bar.addView(row)
        doneBtn = Ui.btn(this, "تم ✓ — أتممت الجلسة") {
            Scheduler.confirm(this, session)
            finish()
        }
        bar.addView(doneBtn)
        settingsBtn = Ui.btn(this, "فتح الإعدادات لاختيار المصحف", true) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        bar.addView(settingsBtn)
        main.addView(bar)

        setContentView(main)
        main.layoutDirection = View.LAYOUT_DIRECTION_RTL
        Ui.applyInsets(main)
    }

    override fun onResume() {
        super.onResume()
        show()
    }

    private fun show() {
        val page = if (idx == 0) first else Prefs.secondPage(first)
        val prefix = if (session in 0..1) Prefs.sessionName(session) + " — " else ""
        titleTv.text = prefix + "صفحة " + page + " (" + (idx + 1) + "/2)"
        prevBtn.isEnabled = idx > 0
        nextBtn.isEnabled = idx < 1
        doneBtn.visibility =
            if (idx == 1 && session in 0..1 && Prefs.isPending(this, session)) View.VISIBLE else View.GONE
        iv.setImageDrawable(null)

        if (!Mushaf.isConfigured(this)) {
            msgTv.text = "لم تختر المصحف بعد. من الإعدادات اختر فولدر صور المصحف أو ملف PDF."
            settingsBtn.visibility = View.VISIBLE
            return
        }
        settingsBtn.visibility = View.GONE
        msgTv.text = "جارٍ التحميل…"
        token++
        val my = token
        val w = resources.displayMetrics.widthPixels
        Thread {
            val b = Mushaf.load(this, page, w)
            runOnUiThread {
                if (isFinishing || isDestroyed || my != token) return@runOnUiThread
                if (b != null) {
                    iv.setImageBitmap(b)
                    msgTv.text = ""
                } else {
                    msgTv.text = "تعذّر عرض صفحة $page — راجع أسماء الملفات أو «إزاحة الترقيم» من الإعدادات."
                    settingsBtn.visibility = View.VISIBLE
                }
            }
        }.start()
    }
}
