package ir.dastyar.eghsat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public class AlarmHelper {
    /** Schedule the selected reminder for the current next installment. */
    public static void schedule(Context c, Installment x) {
        if (x.isFinished() || x.reminderMode == 0) { cancel(c, x.id); return; }

        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);

        // Build the reminder date using calendar days, not milliseconds. This avoids
        // DST/time-zone edge cases when the reminder is 2 or 7 days before due date.
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(x.nextDueMillis());
        if (x.reminderMode == 2) cal.add(java.util.Calendar.DAY_OF_MONTH, -2);
        else if (x.reminderMode == 3) cal.add(java.util.Calendar.DAY_OF_MONTH, -7);

        cal.set(java.util.Calendar.HOUR_OF_DAY, Math.max(0, Math.min(23, x.reminderHour)));
        cal.set(java.util.Calendar.MINUTE, Math.max(0, Math.min(59, x.reminderMinute)));
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long trigger = cal.getTimeInMillis();

        long now = System.currentTimeMillis();
        // Never create a stale alarm for an occurrence whose reminder time has passed.
        if (trigger <= now + 1000L) { cancel(c, x.id); return; }

        Intent i = new Intent(c, ReminderReceiver.class).putExtra("id", x.id);
        PendingIntent pi = PendingIntent.getBroadcast(
                c, (int)(x.id & 0x7fffffff), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Replacing the existing alarm with the same PendingIntent prevents duplicates.
        am.cancel(pi);
        if (android.os.Build.VERSION.SDK_INT >= 31 && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        } else {
            // Works without exact-alarm access; Android may deliver it slightly later.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
        }
    }

    public static void rescheduleAll(Context c) {
        for (Installment x : Store.all(c)) schedule(c, x);
    }

    public static void cancel(Context c, long id) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(c, ReminderReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(
                c, (int) (id & 0x7fffffff), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        am.cancel(pi);
        pi.cancel();
    }
}
