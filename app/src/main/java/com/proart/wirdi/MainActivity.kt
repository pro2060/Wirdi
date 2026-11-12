package com.proart.wirdi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var root: LinearLayout
    private lateinit var progress: ProgressBar
    private lateinit var nextTv: TextView
    private val timeTv = arrayOfNulls<TextView>(2)
    private val statusTv = arrayOfNulls<TextView>(2)
    private val readBtn = arrayOfNulls<MaterialButton>(2)
    private val doneBtn = arrayOfNulls<MaterialButton>(2)

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

        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = Gravity.CENTER_VERTICAL
        val title = Ui.tv(this, "وِرْدي", 34f, true, Ui.GREEN, 0)
        title.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        header.addView(title)
        val settings = Ui.btn(this, "⚙ الإعدادات", false) {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        settings.layoutParams = LinearLayout.LayoutParams(Ui.WRAP, Ui.WRAP)
        header.addView(settings)
        root.addView(header)
        root.addView(Ui.tv(this, "ورد يومي: صفحتان صباحًا وصفحتان مساءً", 14f, false, Ui.MUTED, 0))

        // بطاقة التقدم
        val pc = Ui.card(this)
        pc.addView(Ui.tv(this, "تقدّمك في المصحف", 16f, true, Ui.INK, 8))
        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        progress.max = TOTAL_PAGES
        progress.progressTintList = ColorStateList.valueOf(Ui.GREEN_L)
        progress.layoutParams = LinearLayout.LayoutParams(Ui.MATCH, Ui.dp(this, 10))
        pc.addView(progress)
        nextTv = Ui.tv(this, "", 14f, false, Ui.MUTED, 0)
        nextTv.setPadding(0, Ui.dp(this, 8), 0, 0)
        pc.addView(nextTv)
        root.addView(pc)

        for (s in 0..1) root.addView(sessionCard(s))

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun sessionCard(s: Int): View {
        val card = Ui.card(this)
        card.addView(Ui.tv(this, (if (s == 0) "🌅 " else "🌙 ") + Prefs.sessionName(s), 19f, true, Ui.INK, 2))
        val tt = Ui.tv(this, "", 13f, false, Ui.MUTED, 6)
        timeTv[s] = tt
        card.addView(tt)
        val st = Ui.tv(this, "", 15f, true, Ui.INK, 10)
        statusTv[s] = st
        card.addView(st)

        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        val rb = Ui.btn(this, "📖 اقرأ", false) { openReader(s) }
        val lp1 = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        lp1.marginEnd = Ui.dp(this, 8)
        rb.layoutParams = lp1
        val db = Ui.btn(this, "تم ✓") {
            Scheduler.confirm(this, s)
            refresh()
        }
        db.layoutParams = LinearLayout.LayoutParams(0, Ui.WRAP, 1f)
        readBtn[s] = rb
        doneBtn[s] = db
        row.addView(rb)
        row.addView(db)
        card.addView(row)
        return card
    }

    private fun openReader(s: Int) {
        val first = if (Prefs.isPending(this, s)) Prefs.getStart(this, s) else Prefs.getCursor(this)
        startActivity(
            Intent(this, ReaderActivity::class.java)
                .putExtra("session", s)
                .putExtra("first", first)
        )
    }

    private fun refresh() {
        for (s in 0..1) {
            val t = Prefs.getTime(this, s)
            val p = Prefs.isPending(this, s)
            timeTv[s]!!.text = if (t < 0) "الوقت: غير محدد — حدّده من الإعدادات" else "الوقت: " + Ui.fmtTime(t)
            val st = statusTv[s]!!
            if (p) {
                st.text = "⏰ ورد معلّق: " + Prefs.pagesText(Prefs.getStart(this, s))
                st.setTextColor(Ui.GOLD)
            } else {
                st.text = if (t < 0) "حدّد الوقت لتفعيل التنبيه" else "✓ لا يوجد ورد معلّق"
                st.setTextColor(Ui.GREEN_L)
            }
            doneBtn[s]!!.isEnabled = p
        }
        val cur = Prefs.getCursor(this)
        progress.progress = cur - 1
        nextTv.text = "الصفحة القادمة في الترتيب: $cur من $TOTAL_PAGES"
    }

    override fun onResume() {
        super.onResume()
        Scheduler.ensure(this)
        refresh()
    }
}
