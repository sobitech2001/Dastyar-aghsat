package ir.dastyar.eghsat;

import android.content.Context;
import android.graphics.Color;

public class Theme {
    private static final String PREF = "dastyar_prefs";
    private static final String KEY_DARK = "dark_mode";

    public boolean dark;
    public int bg, card, text, muted, primary, primaryText, success, danger, border;

    public static boolean isDark(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE).getBoolean(KEY_DARK, false);
    }

    public static void setDark(Context c, boolean d) {
        c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putBoolean(KEY_DARK, d).apply();
    }

    public static Theme get(Context c) {
        Theme t = new Theme();
        t.dark = isDark(c);
        if (t.dark) {
            t.bg = Color.rgb(18, 18, 22);
            t.card = Color.rgb(30, 30, 37);
            t.text = Color.rgb(240, 240, 245);
            t.muted = Color.rgb(158, 158, 170);
            t.primary = Color.rgb(146, 132, 255);
            t.primaryText = Color.rgb(18, 18, 22);
            t.success = Color.rgb(72, 201, 146);
            t.danger = Color.rgb(240, 110, 110);
            t.border = Color.rgb(48, 48, 56);
        } else {
            t.bg = Color.rgb(247, 247, 250);
            t.card = Color.WHITE;
            t.text = Color.rgb(23, 23, 28);
            t.muted = Color.rgb(119, 119, 132);
            t.primary = Color.rgb(91, 75, 219);
            t.primaryText = Color.WHITE;
            t.success = Color.rgb(22, 138, 88);
            t.danger = Color.rgb(198, 40, 40);
            t.border = Color.rgb(232, 232, 238);
        }
        return t;
    }
}
