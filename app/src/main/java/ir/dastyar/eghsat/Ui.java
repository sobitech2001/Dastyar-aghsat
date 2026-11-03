package ir.dastyar.eghsat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;

/** Small view/format helpers shared by every screen (single source instead of per-file copies). */
final class Ui {
    private Ui() {}

    static int dp(Context c, float v) { return (int) (v * c.getResources().getDisplayMetrics().density + 0.5f); }

    static GradientDrawable rounded(Context c, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(c, radiusDp));
        return g;
    }

    static GradientDrawable roundedStroke(Context c, int color, int strokeColor, float radiusDp, float strokeDp) {
        GradientDrawable g = rounded(c, color, radiusDp);
        g.setStroke(Math.max(1, dp(c, strokeDp)), strokeColor);
        return g;
    }

    static String money(long n) { return NumberFormat.getInstance(Locale.US).format(n) + " تومان"; }

    /**
     * Keeps only ASCII digits, first converting Persian (۰-۹) and Arabic-Indic (٠-٩) digits.
     * Persian keyboards send their own digit glyphs, so every numeric field must go
     * through this before parsing — otherwise a valid amount silently looks invalid.
     */
    static String digits(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char ch = raw.charAt(i);
            if (ch >= '0' && ch <= '9') sb.append(ch);
            else if (ch >= '\u06F0' && ch <= '\u06F9') sb.append((char) ('0' + (ch - '\u06F0')));
            else if (ch >= '\u0660' && ch <= '\u0669') sb.append((char) ('0' + (ch - '\u0660')));
        }
        return sb.toString();
    }

    /** Converts ASCII digits to Persian digits for display inside the Persian report. */
    static String persianDigits(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            sb.append(ch >= '0' && ch <= '9' ? (char) ('\u06F0' + (ch - '0')) : ch);
        }
        return sb.toString();
    }

    /** Grouped number with Persian digits (e.g. ۵٬۰۰۰٬۰۰۰). */
    static String numberFa(long n) { return persianDigits(NumberFormat.getInstance(Locale.US).format(n)); }

    /** Grouped amount with Persian digits and the currency unit. */
    static String moneyFa(long n) { return numberFa(n) + " تومان"; }

    /** Gregorian millis as a Jalali yyyy/MM/dd string (ASCII digits). */
    static String jalali(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        int[] j = PersianDate.toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
        return String.format(Locale.US, "%04d/%02d/%02d", j[0], j[1], j[2]);
    }

    /** Jalali date with Persian digits, as used in the printed report. */
    static String jalaliFa(long millis) { return persianDigits(jalali(millis)); }

    /** TextView with the explicit sans-serif typeface used by most screens. */
    static TextView tv(Context c, String s, float size, int color) {
        TextView t = plainTv(c, s, size, color);
        t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return t;
    }

    /** TextView that keeps the theme's default typeface. */
    static TextView plainTv(Context c, String s, float size, int color) {
        TextView t = new TextView(c);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        return t;
    }

    static int blend(int foreground, int background, float backgroundWeight) {
        float fw = 1f - backgroundWeight;
        return Color.rgb(
                Math.round(Color.red(foreground) * fw + Color.red(background) * backgroundWeight),
                Math.round(Color.green(foreground) * fw + Color.green(background) * backgroundWeight),
                Math.round(Color.blue(foreground) * fw + Color.blue(background) * backgroundWeight));
    }
}
