package com.proart.wirdi

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton

object Ui {
    const val GREEN = 0xFF1B5E20.toInt()
    const val GREEN_L = 0xFF2E7D32.toInt()
    const val GOLD = 0xFF9A6B00.toInt()
    const val BG = 0xFFF6F3EC.toInt()
    const val INK = 0xFF1F2A24.toInt()
    const val MUTED = 0xFF5F6B64.toInt()
    const val MATCH = ViewGroup.LayoutParams.MATCH_PARENT
    const val WRAP = ViewGroup.LayoutParams.WRAP_CONTENT

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    fun tv(c: Context, text: String, size: Float, bold: Boolean = false, color: Int = INK, bottom: Int = 6): TextView {
        val t = TextView(c)
        t.text = text
        t.textSize = size
        t.setTextColor(color)
        if (bold) t.setTypeface(t.typeface, Typeface.BOLD)
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.bottomMargin = dp(c, bottom)
        t.layoutParams = lp
        return t
    }

    fun btn(c: Context, text: String, filled: Boolean = true, onClick: () -> Unit): MaterialButton {
        val b = if (filled) MaterialButton(c)
        else MaterialButton(c, null, com.google.android.material.R.attr.materialButtonOutlinedStyle)
        b.text = text
        b.isAllCaps = false
        b.cornerRadius = dp(c, 12)
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.bottomMargin = dp(c, 8)
        b.layoutParams = lp
        b.setOnClickListener { onClick() }
        return b
    }

    fun card(c: Context): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        l.setPadding(dp(c, 16), dp(c, 14), dp(c, 16), dp(c, 14))
        val bg = GradientDrawable()
        bg.cornerRadius = dp(c, 18).toFloat()
        bg.setColor(Color.WHITE)
        bg.setStroke(dp(c, 1), 0xFFE3DDCF.toInt())
        l.background = bg
        l.elevation = dp(c, 2).toFloat()
        val lp = LinearLayout.LayoutParams(MATCH, WRAP)
        lp.topMargin = dp(c, 12)
        l.layoutParams = lp
        return l
    }

    fun fmtTime(min: Int): String {
        val h = min / 60
        val m = min % 60
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "$h12:" + "%02d".format(m) + if (h < 12) " ص" else " م"
    }

    /** يحافظ على المحتوى بعيدًا عن شريط الحالة وشريط التنقل (أندرويد 15+). */
    fun applyInsets(v: View) {
        ViewCompat.setOnApplyWindowInsetsListener(v) { view, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(b.left, b.top, b.right, b.bottom)
            insets
        }
    }
}
