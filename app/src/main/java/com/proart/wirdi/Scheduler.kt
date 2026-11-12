package com.proart.wirdi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object Scheduler {
    private const val REPEAT_MS = 15 * 60 * 1000L

    private fun pi(c: Context, s: Int): PendingIntent {
        val i = Intent(c, AlarmReceiver::class.java).putExtra("session", s)
        return PendingIntent.getBroadcast(
            c, 100 + s, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextOccurrence(minutes: Int): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, minutes / 60)
        cal.set(Calendar.MINUTE, minutes % 60)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis() + 1000) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    fun scheduleAt(c: Context, s: Int, whenMs: Long) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val p = pi(c, s)
        am.cancel(p)
        Prefs.setNext(c, s, whenMs)
        val exact = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        try {
            if (exact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, p)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, p)
        } catch (e: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, whenMs, p)
        }
    }

    fun cancel(c: Context, s: Int) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(c, s))
        Prefs.setNext(c, s, 0L)
    }

    fun scheduleDaily(c: Context, s: Int) {
        val t = Prefs.getTime(c, s)
        if (t < 0) { cancel(c, s); return }
        scheduleAt(c, s, nextOccurrence(t))
    }

    fun scheduleRepeat(c: Context, s: Int) = scheduleAt(c, s, System.currentTimeMillis() + REPEAT_MS)

    /** المستخدم أكّد إتمام الجلسة. */
    fun confirm(c: Context, s: Int) {
        Prefs.setPending(c, s, false)
        Notifier.cancel(c, s)
        if (Prefs.getTime(c, s) >= 0) scheduleDaily(c, s) else cancel(c, s)
    }

    /** يعيد تسليح المنبهات (بعد فتح التطبيق / إعادة تشغيل الجهاز / قتل التطبيق). */
    fun ensure(c: Context) {
        val now = System.currentTimeMillis()
        for (s in 0..1) {
            val pending = Prefs.isPending(c, s)
            if (!pending && Prefs.getTime(c, s) < 0) { cancel(c, s); continue }
            val next = Prefs.getNext(c, s)
            when {
                next > now -> scheduleAt(c, s, next)
                next > 0L -> scheduleAt(c, s, now + 3000) // الموعد فات والجهاز كان مغلق
                else -> scheduleDaily(c, s)
            }
        }
    }
}
