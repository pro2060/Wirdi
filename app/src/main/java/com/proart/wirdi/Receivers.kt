package com.proart.wirdi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val s = i.getIntExtra("session", -1)
        if (s !in 0..1) return
        Prefs.beginSession(c, s)       // لو فيه جلسة معلّقة مش هتتغير
        Notifier.show(c, s)
        Scheduler.scheduleRepeat(c, s) // تذكير كل ١٥ دقيقة
    }
}

class ConfirmReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val s = i.getIntExtra("session", -1)
        if (s !in 0..1) return
        Scheduler.confirm(c, s)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Scheduler.ensure(c)
    }
}
