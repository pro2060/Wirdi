package com.proart.wirdi

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.button.MaterialButton

class SettingsActivity : AppCompatActivity() {

    private lateinit var root: LinearLayout
    private lateinit var startEt: EditText
    private lateinit var offsetEt: EditText
    private lateinit var mushafTv: TextView
    private val timeBtn = arrayOfNulls<MaterialButton>(2)

    private val pickFolder =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) onPicked(uri, 1)
        }
    private val pickPdf =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) onPicked(uri, 2)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        scroll.setBackgroundColor(Ui.BG)
        scroll.clipToPadding = false
        root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 16), Ui.dp(this, 32))
        scroll.addView(root)
        setContentView(scroll)
        scroll.layoutDirection = View.LAYOUT_DIRECTION_RTL
        Ui.applyInsets(scroll)

        root.addView(Ui.tv(this, "الإعدادات", 28f, true, Ui.GREEN, 0))

        // ١) الأوقات
        val c1 = Ui.card(this)
        c1.addView(Ui.tv(this, "⏰ أوقات الجلسات", 17f, true))
        for (s in 0..1) {
            c1.addView(Ui.tv(this, Prefs.sessionName(s), 13f, false, Ui.MUTED, 2))
            val b = Ui.btn(this, "", false) { pickTime(s) }
            timeBtn[s] = b
            c1.addView(b)
        }
        root.addView(c1)

        // ٢) الصفحات
        val c2 = Ui.card(this)
        c2.addView(Ui.tv(this, "📄 نقطة البداية", 17f, true))
        c2.addView(Ui.tv(this, "رقم أول صفحة تقرأها في الجلسة القادمة (من ١ إلى ٦٠٤)", 13f, false, Ui.MUTED))
        startEt = EditText(this)
        startEt.inputType = InputType.TYPE_CLASS_NUMBER
        startEt.setTextColor(Ui.INK)
        startEt.gravity = Gravity.START
        c2.addView(startEt)
        c2.addView(Ui.btn(this, "حفظ نقطة البداية") {
            val v = startEt.text.toString().toIntOrNull()
            if (v == null || v < 1 || v > TOTAL_PAGES) {
                Toast.makeText(this, "اكتب رقم بين 1 و 604", Toast.LENGTH_SHORT).show()
            } else {
                Prefs.setCursor(this, v)
                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
            }
        })
        root.addView(c2)

        // ٣) المصحف
        val c3 = Ui.card(this)
        c3.addView(Ui.tv(this, "📖 المصحف داخل التطبيق", 17f, true))
        mushafTv = Ui.tv(this, "", 14f, true, Ui.GOLD)
        c3.addView(mushafTv)
        c3.addView(Ui.tv(this,
            "اختر فولدر فيه صور الصفحات (PNG/JPG/WebP) وأسماؤها تنتهي برقم الصفحة مثل 001.jpg أو page_12.png، " +
                "أو اختر ملف PDF للمصحف. ملفات Illustrator (.ai) لازم تتصدّر لصور أو PDF الأول.", 13f, false, Ui.MUTED))
        c3.addView(Ui.btn(this, "اختيار فولدر صور المصحف") { pickFolder.launch(null) })
        c3.addView(Ui.btn(this, "اختيار ملف PDF للمصحف", false) {
            pickPdf.launch(arrayOf("application/pdf"))
        })
        c3.addView(Ui.tv(this,
            "إزاحة الترقيم: لو أول صفحة من المصحف رقمها في الملفات مش ١، اكتب الفرق (مثلًا 2 لو الصفحة ١ هي الملف رقم 3). الافتراضي 0.",
            13f, false, Ui.MUTED))
        offsetEt = EditText(this)
        offsetEt.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED
        offsetEt.setTextColor(Ui.INK)
        offsetEt.gravity = Gravity.START
        c3.addView(offsetEt)
        c3.addView(Ui.btn(this, "حفظ الإزاحة", false) {
            val v = offsetEt.text.toString().toIntOrNull()
            if (v == null) {
                Toast.makeText(this, "اكتب رقم صحيح", Toast.LENGTH_SHORT).show()
            } else {
                Prefs.setOffset(this, v)
                Toast.makeText(this, "تم الحفظ", Toast.LENGTH_SHORT).show()
            }
        })
        c3.addView(Ui.btn(this, "إزالة المصحف", false) {
            Prefs.setMushaf(this, 0, null)
            Mushaf.reset()
            refresh()
        })
        root.addView(c3)

        // ٤) التنبيهات
        val c4 = Ui.card(this)
        c4.addView(Ui.tv(this, "🔔 التنبيهات", 17f, true))
        c4.addView(Ui.btn(this, "١) السماح بالإشعارات") { askNotifications() })
        c4.addView(Ui.btn(this, "٢) استثناء التطبيق من توفير البطارية") { askBattery() })
        c4.addView(Ui.tv(this,
            "٣) على أوبو: الإعدادات ← إدارة التطبيقات ← وِرْدي ← فعّل «التشغيل التلقائي» و«النشاط في الخلفية»، " +
                "ومن شاشة التطبيقات الأخيرة اسحب أيقونة وِرْدي لأسفل لقفله.", 13f, false, Ui.MUTED))
        c4.addView(Ui.btn(this, "تجربة تنبيه الآن", false) { Notifier.showTest(this) })
        root.addView(c4)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        for (s in 0..1) {
            val t = Prefs.getTime(this, s)
            timeBtn[s]!!.text = if (t < 0) "حدّد الوقت" else Ui.fmtTime(t) + "  (اضغط للتغيير)"
        }
        startEt.setText(Prefs.getCursor(this).toString())
        offsetEt.setText(Prefs.getOffset(this).toString())
        mushafTv.text = when (Prefs.getMushafType(this)) {
            1 -> "✓ مصحف من فولدر صور"
            2 -> "✓ مصحف من ملف PDF"
            else -> "لم يتم اختيار المصحف بعد"
        }
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

    private fun onPicked(uri: Uri, type: Int) {
        try {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) {
        }
        Prefs.setMushaf(this, type, uri.toString())
        Mushaf.reset()
        refresh()
        Toast.makeText(this, "جارٍ فحص المصحف…", Toast.LENGTH_SHORT).show()
        Thread {
            val n = Mushaf.pageCount(this)
            runOnUiThread {
                val msg = if (n > 0) "تم العثور على $n صفحة ✓"
                else "ماتم العثور على صفحات — راجع نوع الملفات وأسماءها"
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
        }.start()
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
}
