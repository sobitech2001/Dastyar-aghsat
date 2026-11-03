package ir.dastyar.eghsat;

import android.content.Context;
import android.graphics.Color;

public class Theme {
    private static final String PREF = "dastyar_prefs";
    private static final String KEY_MODE = "theme_mode";
    public static final int LIGHT = 0;
    public static final int DARK = 1;
    public static final int BLUE = 2;
    public static final int ORANGE = 3;
    public static final int GREEN = 4;

    public boolean dark, blue, orange, green;
    public int mode;
    public int bg, card, text, muted, primary, primaryText, success, danger, border;
    /** colours of the side-menu header gradient and its shade strip */
    public int[] drawerHead, drawerShades;

    public static int getMode(Context c) {
        android.content.SharedPreferences p = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
        if (p.contains(KEY_MODE)) return p.getInt(KEY_MODE, LIGHT);
        return p.getBoolean("dark_mode", false) ? DARK : LIGHT;
    }
    public static void setMode(Context c, int mode) { c.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putInt(KEY_MODE, mode).apply(); }
    public static boolean isDark(Context c) { return getMode(c) == DARK; }
    public static void setDark(Context c, boolean d) { setMode(c, d ? DARK : LIGHT); }

    public static Theme get(Context c) {
        Theme t = new Theme(); t.mode = getMode(c);
        t.dark=t.mode==DARK; t.blue=t.mode==BLUE; t.orange=t.mode==ORANGE; t.green=t.mode==GREEN;
        if (t.dark) {
            t.bg=Color.rgb(10,15,24); t.card=Color.rgb(20,28,40); t.text=Color.rgb(238,245,255); t.muted=Color.rgb(157,174,196);
            t.primary=Color.rgb(72,132,218); t.primaryText=Color.WHITE; t.success=Color.rgb(63,190,137); t.danger=Color.rgb(239,100,112); t.border=Color.rgb(43,58,77);
        } else if (t.blue) {
            t.bg=Color.rgb(7,29,52); t.card=Color.rgb(14,52,86); t.text=Color.rgb(240,248,255); t.muted=Color.rgb(167,201,231);
            t.primary=Color.rgb(33,150,243); t.primaryText=Color.WHITE; t.success=Color.rgb(53,199,145); t.danger=Color.rgb(255,105,120); t.border=Color.rgb(35,91,137);
        } else if (t.orange) {
            t.bg=Color.rgb(252,246,238); t.card=Color.rgb(255,251,245); t.text=Color.rgb(67,42,25); t.muted=Color.rgb(128,99,76);
            t.primary=Color.rgb(198,96,31); t.primaryText=Color.WHITE; t.success=Color.rgb(69,126,74); t.danger=Color.rgb(190,55,45); t.border=Color.rgb(235,211,188);
        } else if (t.green) {
            t.bg=Color.rgb(242,249,245); t.card=Color.rgb(252,255,253); t.text=Color.rgb(24,55,39); t.muted=Color.rgb(91,121,104);
            t.primary=Color.rgb(46,125,82); t.primaryText=Color.WHITE; t.success=Color.rgb(30,133,80); t.danger=Color.rgb(194,65,65); t.border=Color.rgb(202,225,211);
        } else {
            t.bg=Color.rgb(247,249,252); t.card=Color.WHITE; t.text=Color.rgb(24,35,50); t.muted=Color.rgb(103,119,139);
            t.primary=Color.rgb(45,91,153); t.primaryText=Color.WHITE; t.success=Color.rgb(22,138,88); t.danger=Color.rgb(198,40,40); t.border=Color.rgb(220,228,238);
        }
        if (t.orange) {
            t.drawerHead=new int[]{0xFF6B2E13,0xFF9B451D,0xFFD06B2C,0xFFF2B56B};
            t.drawerShades=new int[]{0xFF5D2810,0xFF8D3D18,0xFFB85B25,0xFFD87B35,0xFFE9A45F,0xFFF5C994,0xFFFFE9D6};
        } else if (t.green) {
            t.drawerHead=new int[]{0xFF174A31,0xFF236B47,0xFF3C8A5A,0xFF8BCF9E};
            t.drawerShades=new int[]{0xFF16422C,0xFF215E3D,0xFF2F7A4D,0xFF459B61,0xFF6EBF82,0xFFA4D8B2,0xFFE4F5EA};
        } else if (t.dark) {
            t.drawerHead=new int[]{0xFF0B1422,0xFF18263B,0xFF274A78,0xFF4B78B5};
            t.drawerShades=new int[]{0xFF08111E,0xFF13253B,0xFF1C3858,0xFF2D5A87,0xFF4B78B5,0xFF739AC6,0xFFB2C8DF};
        } else if (t.blue) {
            t.drawerHead=new int[]{0xFF061B31,0xFF0C3765,0xFF1769AA,0xFF64B5F6};
            t.drawerShades=new int[]{0xFF061B31,0xFF0C3765,0xFF1769AA,0xFF1976D2,0xFF42A5F5,0xFF90CAF9,0xFFE3F2FD};
        } else {
            t.drawerHead=new int[]{0xFF234A78,0xFF2E659F,0xFF4B86C5,0xFF8DB9E5};
            t.drawerShades=new int[]{0xFF234A78,0xFF2E659F,0xFF4B86C5,0xFF6E9DD0,0xFF8DB9E5,0xFFB9D0E8,0xFFE7F0F8};
        }
        return t;
    }
}
