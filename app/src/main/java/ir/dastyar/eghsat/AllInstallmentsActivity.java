package ir.dastyar.eghsat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.widget.*;
import java.util.*;

/**
 * Full list of every installment (active or finished), unlike the home
 * screen which only surfaces the ones due soon. Lets the user browse,
 * pay, edit or delete anything from one place.
 */
public class AllInstallmentsActivity extends Activity {
    LinearLayout list, statsBox;
    Theme th;
    // 0 = active, 1 = finished, 2 = all
    int filter = 0;
    TextView tabActive, tabFinished, tabAll;

    int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }

    TextView tv(String s, float size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        return t;
    }

    GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radiusDp));
        return g;
    }

    GradientDrawable roundedStroke(int color, int strokeColor, float radiusDp, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radiusDp));
        g.setStroke(Math.max(1,dp(strokeDp)), strokeColor);
        return g;
    }

    String money(long n) { return java.text.NumberFormat.getInstance(Locale.US).format(n) + " تومان"; }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.get(this);
        getWindow().setStatusBarColor(th.bg);

        FrameLayout outer = new FrameLayout(this);
        outer.setBackgroundColor(th.bg);

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(16),dp(16),dp(12));

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = tv("←", 22, th.text);
        back.setPadding(dp(6),dp(6),dp(14),dp(6));
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(-2,-2));
        LinearLayout titleCol = new LinearLayout(this); titleCol.setOrientation(LinearLayout.VERTICAL);
        TextView title = tv("داشبورد اقساط", 21, th.text);
        title.setTypeface(null, 1);
        titleCol.addView(title);
        TextView subtitle = tv("مدیریت و مرور کامل اقساط", 13, th.muted);
        titleCol.addView(subtitle);
        top.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(-1,-2);
        topLp.setMargins(0,0,0,dp(14));
        root.addView(top, topLp);

        statsBox = new LinearLayout(this); statsBox.setOrientation(LinearLayout.HORIZONTAL);
        statsBox.setBackground(rounded(th.primary, 16));
        statsBox.setPadding(dp(14),dp(14),dp(14),dp(14));
        LinearLayout.LayoutParams statsLp = new LinearLayout.LayoutParams(-1,-2);
        statsLp.setMargins(0,0,0,dp(14));
        root.addView(statsBox, statsLp);

        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        tabActive = tab("فعال", 0);
        tabFinished = tab("تکمیل‌شده", 1);
        tabAll = tab("همه", 2);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0,dp(38),1f);
        tp.setMargins(0,0,dp(8),0);
        tabs.addView(tabActive, tp);
        LinearLayout.LayoutParams tp2 = new LinearLayout.LayoutParams(0,dp(38),1f);
        tp2.setMargins(0,0,dp(8),0);
        tabs.addView(tabFinished, tp2);
        tabs.addView(tabAll, new LinearLayout.LayoutParams(0,dp(38),1f));
        LinearLayout.LayoutParams tabsLp = new LinearLayout.LayoutParams(-1,-2);
        tabsLp.setMargins(0,0,0,dp(14));
        root.addView(tabs, tabsLp);

        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0,0,0,dp(24));
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1,0,1));

        outer.addView(root, new FrameLayout.LayoutParams(-1,-1));

        outer.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets sb = insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(dp(16), sb.top + dp(16), dp(16), sb.bottom + dp(12));
            return insets;
        });

        setContentView(outer);
        refresh();
    }

    TextView tab(String label, int idx) {
        TextView t = tv(label, 13, th.text);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(null, 1);
        t.setOnClickListener(v -> { filter = idx; refresh(); });
        return t;
    }

    void restyleTabs() {
        TextView[] all = {tabActive, tabFinished, tabAll};
        for (int i = 0; i < all.length; i++) {
            boolean on = (i == filter);
            all[i].setTextColor(on ? th.primaryText : th.text);
            all[i].setBackground(on ? rounded(th.primary, 10) : roundedStroke(th.card, th.border, 10, 1));
        }
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    void statCell(String label, String value) {
        LinearLayout col = new LinearLayout(this); col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER);
        TextView v = tv(value, 16, th.primaryText); v.setTypeface(null,1); v.setGravity(Gravity.CENTER);
        TextView l = tv(label, 12, th.primaryText); l.setAlpha(0.85f); l.setGravity(Gravity.CENTER);
        col.addView(v); col.addView(l);
        statsBox.addView(col, new LinearLayout.LayoutParams(0,-2,1));
    }

    void renderStats(List<Installment> all) {
        statsBox.removeAllViews();
        int active = 0, finished = 0;
        long totalDebt = 0;
        for (Installment x : all) {
            if (x.isFinished()) finished++;
            else { active++; totalDebt += x.amount * (x.totalCount - x.paidCount); }
        }
        statCell("فعال", String.valueOf(active));
        statCell("تکمیل‌شده", String.valueOf(finished));
        statCell("مانده کل", money(totalDebt));
    }

    void refresh() {
        restyleTabs();
        list.removeAllViews();
        List<Installment> all = Store.all(this);
        renderStats(all);
        List<Installment> shown = new ArrayList<>();
        for (Installment x : all) {
            if (filter == 0 && !x.isFinished()) shown.add(x);
            else if (filter == 1 && x.isFinished()) shown.add(x);
            else if (filter == 2) shown.add(x);
        }
        Collections.sort(shown, (a,b) -> {
            if (a.isFinished() != b.isFinished()) return a.isFinished() ? 1 : -1;
            return Long.compare(a.nextDueMillis(), b.nextDueMillis());
        });

        if (shown.isEmpty()) {
            String msg = filter == 1 ? "هنوز قسطی تکمیل نشده." : "قسطی در این بخش نیست.";
            TextView e = tv(msg, 16, th.muted);
            e.setGravity(Gravity.CENTER);
            list.addView(e, new LinearLayout.LayoutParams(-1,dp(180)));
            return;
        }
        for (Installment x : shown) {
            list.addView(CardBuilder.build(this, th, x, new CardBuilder.Actions() {
                @Override public void onPay(Installment y) {
                    y.paidCount++;
                    if (y.paidCount >= y.totalCount) y.active = false;
                    Store.upsert(AllInstallmentsActivity.this, y);
                    PaymentLog.record(AllInstallmentsActivity.this, y.id, y.amount);
                    AlarmHelper.schedule(AllInstallmentsActivity.this, y);
                    refresh();
                }
                @Override public void onEdit(Installment y) {
                    startActivity(new Intent(AllInstallmentsActivity.this, AddInstallmentActivity.class).putExtra("id", y.id));
                }
                @Override public void onDelete(Installment y) { confirmDelete(y); }
            }));
        }
    }

    void confirmDelete(Installment x) {
        new AlertDialog.Builder(this)
                .setTitle("حذف قسط")
                .setMessage("«" + x.title + "» حذف بشه؟ این کار قابل بازگشت نیست.")
                .setPositiveButton("حذف", (d,w) -> {
                    AlarmHelper.cancel(this, x.id);
                    Store.delete(this, x.id);
                    PaymentLog.deleteForInstallment(this, x.id);
                    refresh();
                })
                .setNegativeButton("انصراف", null)
                .show();
    }
}
