package ir.dastyar.eghsat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Locale;

/** Small, theme-aware reminder editor used from every installment card. */
public final class ReminderDialog {
    private ReminderDialog() {}

    static int dp(Activity a, float v) { return Ui.dp(a, v); }
    static GradientDrawable rounded(Activity a, int color, float r) { return Ui.rounded(a, color, r); }
    static GradientDrawable stroke(Activity a,int color,int border,float r){ return Ui.roundedStroke(a, color, border, r, 2); }

    public static void show(Activity a, Theme th, Installment x) { show(a, th, x, null); }

    public static void show(Activity a, Theme th, Installment x, Runnable onSaved) {
        LinearLayout box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(a,18), dp(a,4), dp(a,18), dp(a,4));

        TextView info = new TextView(a);
        info.setText("برای «" + x.title + "» زمان ارسال یادآور را انتخاب کنید.");
        info.setTextSize(14); info.setTextColor(th.muted); info.setPadding(0,dp(a,4),0,dp(a,12));
        box.addView(info);

        TextView label = new TextView(a); label.setText("زمان یادآوری"); label.setTextSize(13); label.setTextColor(th.muted);
        box.addView(label);
        Spinner mode = new Spinner(a);
        String[] modes = {"بدون یادآور", "زمان سررسید", "دو روز مانده", "یک هفته مانده"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(a, android.R.layout.simple_spinner_item, modes) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v=(TextView)super.getView(position,convertView,parent);v.setTextColor(th.text);v.setTextSize(14);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(a,12),dp(a,7),dp(a,12),dp(a,7));return v;
            }
            @Override public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v=(TextView)super.getDropDownView(position,convertView,parent);v.setTextColor(th.text);v.setTextSize(14);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(a,14),dp(a,10),dp(a,14),dp(a,10));v.setBackgroundColor(th.card);return v;
            }
        };
        mode.setAdapter(adapter); mode.setPopupBackgroundDrawable(rounded(a,th.card,16)); mode.setBackground(stroke(a,th.card,th.border,16));
        mode.setSelection(Math.max(0, Math.min(3,x.reminderMode)));
        box.addView(mode,new LinearLayout.LayoutParams(-1,dp(a,52)));

        TextView time = new TextView(a);
        final int[] hm = {x.reminderHour, x.reminderMinute};
        time.setText(String.format(Locale.US,"ساعت ارسال: %02d:%02d",hm[0],hm[1]));
        time.setTextSize(14); time.setTextColor(th.text); time.setGravity(Gravity.CENTER);
        time.setBackground(stroke(a,th.card,th.border,16));
        LinearLayout.LayoutParams timeLp=new LinearLayout.LayoutParams(-1,dp(a,48));timeLp.setMargins(0,dp(a,10),0,0);box.addView(time,timeLp);
        time.setOnClickListener(v -> new TimePickerDialog(a,(view,hour,minute)->{hm[0]=hour;hm[1]=minute;time.setText(String.format(Locale.US,"ساعت ارسال: %02d:%02d",hour,minute));},hm[0],hm[1],true).show());

        TextView hint=new TextView(a);hint.setText("یادآور فقط برای قسط بعدی فعال می‌شود و بعد از پرداخت، خودکار برای سررسید بعدی تنظیم می‌شود.");hint.setTextSize(12);hint.setTextColor(th.muted);hint.setPadding(0,dp(a,10),0,dp(a,2));box.addView(hint);

        AlertDialog dialog=new AlertDialog.Builder(a).setTitle("یادآور قسط").setView(box).setNegativeButton("انصراف",null).setPositiveButton("ذخیره",null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(btn -> {
            x.reminderMode=mode.getSelectedItemPosition();
            x.reminderHour=hm[0]; x.reminderMinute=hm[1];
            Store.upsert(a,x);
            if(x.reminderMode==0) AlarmHelper.cancel(a,x.id); else AlarmHelper.schedule(a,x);
            Toast.makeText(a,x.reminderMode==0?"یادآور خاموش شد":"یادآور ذخیره شد",Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            if (onSaved != null) onSaved.run();
        }));
        dialog.show();
    }
}
