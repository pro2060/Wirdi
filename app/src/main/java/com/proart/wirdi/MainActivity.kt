package com.proart.wirdi

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var root: LinearLayout
    private lateinit var nextTv: TextView
    private val timeBtn = arrayOfNulls<MaterialButton>(2)
    private val statusTv = arrayOfNulls<TextView>(2)
    private val doneBtn = arrayOfNulls<MaterialButton>(2)

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    private val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    private fun tv(text: String, size: Float, bold: Boolean = false, color: Int = 0xFF1B1B1B.toInt()): TextView =
        TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(6) }
        }

    private fun btn(text: String, onClick: () -> Unit): MaterialButton =
        MaterialButton(this).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { bottomMargin = dp(8) }
            setOnClickListener { onClick() }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(32))
        }
        scroll.addView(root)
        setContentView(scroll)
        scroll.layoutDirection = View.LAYOUT_DIRECTION_RTL

        root.addView(tv("وِرْدي", 30f, true, 0xFF1B5E20.toInt()))
        root.addView(tv("٤ صفحات يوميًا: صفحتان صباحًا وصفحتان مساءً", 14f))
        nextTv = tv("", 14f, true)
        root.addView(nextTv)

        for (s in 0..1) root.addView(card(s))

        val startLabel = tv("نقطة البداية (رقم الصفحة من ١ إلى ٦٠٤)", 14f, true)
        (startLabel.layoutParams as LinearLayout.LayoutParams).topMargin = dp(12)
        root.addView(startLabel)
        val startEt = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "مثال: 1"
            setText(Prefs.getCursor(this@MainActivity).toString())
            gravity = Gravity.START
        }
        root.addView(startEt)
        root.addView(btn("حفظ نقطة البداية") {
            val v = startEt.text.toString().toIntOrNull()
            if (v == null || v < 1 || v > TOTAL_PAGES) {
                Toast.makeText(this, "اكتب رقم بين 1 و 604", Toast.LENGTH_SHORT).show()
            } else {
                Prefs.setCursor(this, v)
                refresh()
                Toast.makeText(this, "تم الحفظ — يسري على الجلسات القادمة", Toast.LENGTH_SHORT).show()
            }
        })

        val setLabel = tv("الإعدادات المهمة للتنبيهات", 16f, true)
        (setLabel.layoutParams as LinearLayout.LayoutParams).topMargin = dp(16)
        root.addView(setLabel)
        root.addView(btn("١) السماح بالإشعارات") { askNotifications() })
        root.addView(btn("٢) استثناء التطبيق من توفير البطارية") { askBattery() })
        root.addView(tv(
            "٣) على أوبو: الإعدادات ← إدارة التطبيقات ← وِرْدي ← فعّل «التشغيل التلقائي» و«النشاط في الخلفية»، " +
                "ثم من شاشة التطبيقات الأخيرة اسحب أيقونة وِرْدي لأسفل لقفله.", 13f))
        root.addView(btn("تجربة تنبيه الآن") { Notifier.showTest(this) })

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) askNotifications()
    }

    private fun card(s: Int): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setStroke(dp(1), 0xFF2E7D32.toInt())
                setColor(0xFFF1F8F4.toInt())
            }
            layoutParams = LinearLayout.LayoutParams(MATCH, WRAP).apply { topMargin = dp(12) }
        }
        card.addView(tv(Prefs.sessionName(s), 18f, true))
        val st = tv("", 14f)
        statusTv[s] = st
        card.addView(st)
        val tb = btn("") { pickTime(s) }
        timeBtn[s] = tb
        card.addView(tb)
        val db = btn("تم ✓ — أتممت الجلسة") {
            Scheduler.confirm(this, s)
            refresh()
        }
        doneBtn[s] = db
        card.addView(db)
        return card
    }

    private fun pickTime(s: Int) {
        val saved = Prefs.getTime(this, s)
        val t = if (saved < 0) (if (s == 0) 6 * 60 else 20 * 60) else saved
        TimePickerDialog(this, { _, h, m ->
            Prefs.setTime(this, s, h * 60 + m)
            if (!Prefs.isPending(this, s)) Scheduler.scheduleDaily(this, s)
            refresh()
        }, t / 60, t % 60, false).show()
    }

    private fun fmt(min: Int): String {
        val h = min / 60
        val m = min % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12:" + "%02d".format(m) + if (h < 12) " ص" else " م"
    }

    private fun refresh() {
        for (s in 0..1) {
            val t = Prefs.getTime(this, s)
            val p = Prefs.isPending(this, s)
            timeBtn[s]!!.text = if (t < 0) "حدّد وقت الجلسة" else "الوقت: " + fmt(t) + "  (اضغط للتغيير)"
            statusTv[s]!!.text = when {
                p -> "⏰ ورد معلّق: " + Prefs.pagesText(Prefs.getStart(this, s)) + " — التذكير كل ١٥ دقيقة"
                t < 0 -> "حدّد الوقت لتفعيل التنبيه"
                else -> "✓ لا يوجد ورد معلّق — التنبيه في الموعد"
            }
            doneBtn[s]!!.isEnabled = p
        }
        nextTv.text = "الصفحة القادمة في الترتيب: " + Prefs.getCursor(this)
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        } else {
            startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            )
        }
    }

    private fun askBattery() {
        try {
            startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            )
        } catch (e: Exception) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        Scheduler.ensure(this)
        refresh()
    }
}
