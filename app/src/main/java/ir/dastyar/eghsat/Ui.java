package ir.dastyar.eghsat;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;
import java.text.NumberFormat;
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
