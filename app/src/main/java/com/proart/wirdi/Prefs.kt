package com.proart.wirdi

import android.content.Context

const val SESSION_MORNING = 0
const val SESSION_EVENING = 1
const val TOTAL_PAGES = 604

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("wirdi", Context.MODE_PRIVATE)

    // وقت الجلسة بالدقائق من منتصف الليل، -1 = لم يُحدد
    fun getTime(c: Context, s: Int): Int = sp(c).getInt("time_$s", -1)
    fun setTime(c: Context, s: Int, minutes: Int) = sp(c).edit().putInt("time_$s", minutes).apply()

    fun isPending(c: Context, s: Int): Boolean = sp(c).getBoolean("pending_$s", false)
    fun setPending(c: Context, s: Int, v: Boolean) = sp(c).edit().putBoolean("pending_$s", v).apply()

    fun getStart(c: Context, s: Int): Int = sp(c).getInt("start_$s", 1)
    fun setStart(c: Context, s: Int, p: Int) = sp(c).edit().putInt("start_$s", p).apply()

    fun getCursor(c: Context): Int = sp(c).getInt("cursor", 1)
    fun setCursor(c: Context, p: Int) = sp(c).edit().putInt("cursor", p).apply()

    fun getNext(c: Context, s: Int): Long = sp(c).getLong("next_$s", 0L)
    fun setNext(c: Context, s: Int, t: Long) = sp(c).edit().putLong("next_$s", t).apply()

    fun secondPage(p: Int): Int = p % TOTAL_PAGES + 1
    fun pagesText(p: Int): String = "صفحة $p و${secondPage(p)}"

    /** تبدأ جلسة جديدة (تحجز صفحتين من المؤشر) فقط لو مفيش جلسة معلّقة. */
    fun beginSession(c: Context, s: Int) {
        if (isPending(c, s)) return
        val cur = getCursor(c)
        setStart(c, s, cur)
        setPending(c, s, true)
        setCursor(c, secondPage(cur) % TOTAL_PAGES + 1)
    }

    fun sessionName(s: Int) = if (s == SESSION_MORNING) "الجلسة الصباحية" else "الجلسة المسائية"
}
