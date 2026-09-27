package ir.dastyar.eghsat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public class AlarmHelper {
    public static void schedule(Context c, Installment x) {
        if (x.isFinished()) return;
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        long trigger = x.nextDueMillis() - 3L*24*60*60*1000;
        if (trigger < System.currentTimeMillis()+5000) trigger = System.currentTimeMillis()+5000;
        Intent i = new Intent(c, ReminderReceiver.class).putExtra("id", x.id);
        PendingIntent pi = PendingIntent.getBroadcast(c, (int)(x.id & 0x7fffffff), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
    }

    public static void rescheduleAll(Context c) {
        for (Installment x : Store.all(c)) schedule(c, x);
    }

    public static void cancel(Context c, long id) {
        AlarmManager am = (AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(c, (int)(id & 0x7fffffff), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        pi.cancel();
    }
}
