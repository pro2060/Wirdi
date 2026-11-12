package com.proart.wirdi

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object Notifier {
    private const val CH = "wird_alarm"

    private fun ensureChannel(c: Context) {
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CH) == null) {
            val ch = NotificationChannel(CH, "تنبيهات الورد", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "تذكير الورد كل ١٥ دقيقة حتى تأكيد الإتمام"
            ch.enableVibration(true)
            ch.vibrationPattern = longArrayOf(0, 400, 200, 400)
            nm.createNotificationChannel(ch)
        }
    }

    private fun canPost(c: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(c, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun show(c: Context, s: Int) {
        ensureChannel(c)
        if (!canPost(c)) return
        val pages = Prefs.pagesText(Prefs.getStart(c, s))
        val open = PendingIntent.getActivity(
            c, 200 + s, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val done = PendingIntent.getBroadcast(
            c, 300 + s, Intent(c, ConfirmReceiver::class.java).putExtra("session", s),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val n = NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("وِرْدي — " + Prefs.sessionName(s))
            .setContentText("حان وقت وردك: $pages")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(open)
            .addAction(0, "تم ✓", done)
            .build()
        val nm = NotificationManagerCompat.from(c)
        nm.cancel(1000 + s)
        nm.notify(1000 + s, n)
    }

    fun showTest(c: Context) {
        ensureChannel(c)
        if (!canPost(c)) return
        val n = NotificationCompat.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("وِرْدي — تجربة")
            .setContentText("التنبيهات شغالة ✓")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(c).notify(999, n)
    }

    fun cancel(c: Context, s: Int) = NotificationManagerCompat.from(c).cancel(1000 + s)
}
