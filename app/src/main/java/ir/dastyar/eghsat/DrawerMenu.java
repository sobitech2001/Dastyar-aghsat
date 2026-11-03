package ir.dastyar.eghsat;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The slide-in side menu of the home screen (tools + theme picker). */
class DrawerMenu {
    private final MainActivity act;
    private final FrameLayout outer;
    private Theme th;
    private FrameLayout drawer;
    private View scrim;
    private boolean open = false;

    DrawerMenu(MainActivity act, FrameLayout outer, Theme th) { this.act = act; this.outer = outer; this.th = th; }

    boolean isOpen() { return open; }

    private int dp(float v) { return Ui.dp(act, v); }
    private TextView tv(String s, float size, int color) { return Ui.tv(act, s, size, color); }
    private GradientDrawable rounded(int color, float r) { return Ui.rounded(act, color, r); }
    private GradientDrawable stroke(int color, int border, float r) { return Ui.roundedStroke(act, color, border, r, 1); }

    void open() {
        if (open) return; open = true;
        scrim = new View(act); scrim.setBackgroundColor(0x99000000); scrim.setOnClickListener(v -> close());
        outer.addView(scrim, new FrameLayout.LayoutParams(-1, -1)); scrim.bringToFront();
        drawer = new FrameLayout(act); drawer.setBackground(rounded(th.bg, 0)); drawer.setElevation(dp(18));
        int width = (int) (act.getResources().getDisplayMetrics().widthPixels * 0.68f);
        FrameLayout.LayoutParams dpLp = new FrameLayout.LayoutParams(width, -1); dpLp.gravity = Gravity.LEFT;
        outer.addView(drawer, dpLp); drawer.bringToFront();
        build(); drawer.setTranslationX(-width); drawer.animate().translationX(0).setDuration(220).start();
    }

    void close() {
        if (!open) return; open = false;
        if (drawer != null) {
            int w = drawer.getWidth();
            drawer.animate().translationX(-w).setDuration(180).withEndAction(() -> {
                outer.removeView(drawer); outer.removeView(scrim); drawer = null; scrim = null;
            }).start();
        }
    }

    private TextView menuItem(String icon, String label) {
        TextView t = tv(icon + "   " + label, 15, th.text); t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        t.setPadding(dp(18), 0, dp(18), 0); t.setBackground(stroke(th.card, th.border, 18)); return t;
    }

    private void addMenuItem(LinearLayout box, String icon, String label, View.OnClickListener click) {
        TextView t = menuItem(icon, label); t.setOnClickListener(v -> { close(); click.onClick(v); });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(52)); lp.setMargins(0, 0, 0, dp(10)); box.addView(t, lp);
    }

    private void build() {
        LinearLayout panel = new LinearLayout(act); panel.setOrientation(LinearLayout.VERTICAL); panel.setPadding(dp(14), dp(28), dp(14), dp(18));
        GradientDrawable headBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, th.drawerHead); headBg.setCornerRadius(dp(26));
        LinearLayout head = new LinearLayout(act); head.setOrientation(LinearLayout.VERTICAL); head.setPadding(dp(18), dp(18), dp(18), dp(18)); head.setBackground(headBg);
        TextView ht = tv("منوی اقساط", 20, Color.WHITE); ht.setTypeface(null, 1); head.addView(ht);
        TextView hs = tv("مدیریت، تقویم، محاسبه و ظاهر برنامه", 12, 0xE6FFFFFF); hs.setPadding(0, dp(5), 0, 0); head.addView(hs);
        LinearLayout shades = new LinearLayout(act); shades.setPadding(0, dp(14), 0, 0);
        for (int c : th.drawerShades) {
            View v = new View(act); v.setBackground(rounded(c, 4));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(6), 1); lp.setMargins(dp(2), 0, dp(2), 0); shades.addView(v, lp);
        }
        head.addView(shades);
        panel.addView(head, new LinearLayout.LayoutParams(-1, dp(142)));

        TextView sec = tv("ابزار", 13, th.muted); sec.setTypeface(null, 1); sec.setPadding(dp(4), dp(18), dp(4), dp(8)); panel.addView(sec);
        addMenuItem(panel, "📋", "داشبورد مدیریت اقساط", v -> act.startActivity(new Intent(act, AllInstallmentsActivity.class)));
        addMenuItem(panel, "📅", "تقویم اقساط", v -> act.startActivity(new Intent(act, CalendarActivity.class)));
        addMenuItem(panel, "🧮", "ماشین حساب اقساط", v -> act.startActivity(new Intent(act, InstallmentCalculatorActivity.class)));
        addMenuItem(panel, "＋", "تعریف اقساط", v -> act.startActivity(new Intent(act, AddInstallmentActivity.class)));

        TextView themeTitle = tv("ظاهر برنامه", 13, th.muted); themeTitle.setTypeface(null, 1); themeTitle.setPadding(dp(4), dp(8), dp(4), dp(8)); panel.addView(themeTitle);
        // Show every theme without horizontal scrolling, in a compact 2-column grid.
        GridLayout themes = new GridLayout(act);
        themes.setColumnCount(2);
        themes.setRowCount(3);
        themes.setUseDefaultMargins(false);
        addThemeButton(themes, "☀", "روشن", Theme.LIGHT);
        addThemeButton(themes, "☾", "تاریک", Theme.DARK);
        addThemeButton(themes, "🔵", "آبی", Theme.BLUE);
        addThemeButton(themes, "🟠", "نارنجی", Theme.ORANGE);
        addThemeButton(themes, "🟢", "سبز", Theme.GREEN);
        LinearLayout.LayoutParams themeGridLp = new LinearLayout.LayoutParams(-1, dp(220));
        themeGridLp.setMargins(0, 0, 0, dp(6));
        panel.addView(themes, themeGridLp);
        drawer.addView(panel, new FrameLayout.LayoutParams(-1, -1));
    }

    private void addThemeButton(GridLayout box, String icon, String label, int mode) {
        LinearLayout b = new LinearLayout(act); b.setOrientation(LinearLayout.VERTICAL); b.setGravity(Gravity.CENTER); b.setPadding(dp(4), dp(6), dp(4), dp(6));
        int base; int txt; int iconColor;
        if (mode == Theme.BLUE) { base = 0xFF0E3456; txt = Color.WHITE; iconColor = 0xFF64B5F6; }
        else if (mode == Theme.DARK) { base = 0xFF1E2938; txt = Color.WHITE; iconColor = Color.WHITE; }
        else if (mode == Theme.ORANGE) { base = 0xFFFFE9D6; txt = 0xFF7A3E18; iconColor = 0xFFD06B2C; }
        else if (mode == Theme.GREEN) { base = 0xFFE4F5EA; txt = 0xFF205A38; iconColor = 0xFF3C8A5A; }
        else { base = Color.WHITE; txt = th.text; iconColor = txt; }
        b.setBackground(stroke(base, mode == th.mode ? th.primary : th.border, 16));
        TextView i = tv(icon, 20, iconColor); i.setGravity(Gravity.CENTER); b.addView(i);
        TextView l = tv(label, 11, txt); l.setGravity(Gravity.CENTER); b.addView(l);
        b.setOnClickListener(v -> act.onThemePicked(mode));
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
        lp.width = 0; lp.height = dp(76); lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        lp.setMargins(dp(3), dp(3), dp(3), dp(3));
        box.addView(b, lp);
    }
}
