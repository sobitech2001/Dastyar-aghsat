package ir.dastyar.eghsat;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL = "installment_reminders";

    @Override public void onReceive(Context context, Intent intent) {
        long id = intent.getLongExtra("id", -1);
        Installment x = Store.find(context, id);
        if (x == null || x.isFinished() || x.reminderMode == 0) return;

        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "یادآوری اقساط",
                    NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(ch);
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(context, (int)id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        android.app.Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new android.app.Notification.Builder(context, CHANNEL)
                : new android.app.Notification.Builder(context);
        b.setSmallIcon(android.R.drawable.ic_dialog_info)
         .setContentTitle("یادآوری قسط «" + x.title + "»")
         .setContentText(x.reminderMode == 1 ? "امروز سررسید قسط است — مبلغ " + x.amount + " تومان" : (x.reminderMode == 2 ? "دو روز تا سررسید مانده — مبلغ " + x.amount + " تومان" : "یک هفته تا سررسید مانده — مبلغ " + x.amount + " تومان"))
         .setAutoCancel(true)
         .setContentIntent(pi);
        nm.notify((int)(id % Integer.MAX_VALUE), b.build());

        // Keep the reminder chain alive for the next unpaid installment.
        // If this installment is still the same/overdue one, schedule() safely cancels it.
        AlarmHelper.schedule(context, x);
    }
}
