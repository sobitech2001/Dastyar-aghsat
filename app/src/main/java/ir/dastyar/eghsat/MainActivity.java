package ir.dastyar.eghsat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout list, root;
    LinearLayout summaryBox;
    Theme th;
    View fab;
    final int REQ = 90;

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

    String money(long n) { return NumberFormat.getInstance(Locale.US).format(n) + " تومان"; }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.get(this);
        getWindow().setStatusBarColor(th.bg);
        build();
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ);
    }

    void build() {
        FrameLayout outer = new FrameLayout(this);
        outer.setBackgroundColor(th.bg);

        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16),dp(16),dp(16),dp(12));

        // top bar: title + calendar + manage-all + theme toggle
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout themeBtn = new LinearLayout(this);
        themeBtn.setOrientation(LinearLayout.HORIZONTAL);
        themeBtn.setGravity(Gravity.CENTER);
        themeBtn.setBackground(rounded(th.card, 20));
        themeBtn.setPadding(dp(12),0,dp(12),0);
        TextView themeIcon = tv(th.dark ? "☀️" : "🌙", 16, th.text);
        TextView themeLabel = tv(th.dark ? "تاریک" : "روشن", 12, th.text);
        LinearLayout.LayoutParams themeLabelLp = new LinearLayout.LayoutParams(-2,-2);
        themeLabelLp.setMarginStart(dp(6));
        themeBtn.addView(themeIcon, new LinearLayout.LayoutParams(-2,-2));
        themeBtn.addView(themeLabel, themeLabelLp);
        themeBtn.setOnClickListener(v -> { Theme.setDark(this, !th.dark); recreate(); });
        LinearLayout.LayoutParams themeLp = new LinearLayout.LayoutParams(-2, dp(40));
        themeLp.setMarginEnd(dp(8));
        top.addView(themeBtn, themeLp);

        TextView title = tv("دستیار اقساط", 18, th.text);
        title.setTypeface(null, 1);
        top.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        TextView calBtn = tv("📅  تقویم", 13, th.text);
        calBtn.setGravity(Gravity.CENTER);
        calBtn.setBackground(rounded(th.card, 22));
        calBtn.setOnClickListener(v -> startActivity(new Intent(this, CalendarActivity.class)));
        LinearLayout.LayoutParams calLp = new LinearLayout.LayoutParams(dp(82), dp(44));
        calLp.setMarginEnd(dp(8));
        top.addView(calBtn, calLp);

        TextView allBtn = tv("📋  داشبورد اقساط", 14, th.text);
        allBtn.setGravity(Gravity.CENTER);
        allBtn.setTypeface(null, 1);
        allBtn.setBackground(rounded(th.card, 20));
        allBtn.setOnClickListener(v -> startActivity(new Intent(this, AllInstallmentsActivity.class)));
        LinearLayout.LayoutParams allLp = new LinearLayout.LayoutParams(dp(142), dp(44));
        allLp.setMarginEnd(dp(8));
        top.addView(allBtn, allLp);

        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(-1,-2);
        topLp.setMargins(0,0,0,dp(16));
        root.addView(top, topLp);

        // summary card (filled in refresh())
        summaryBox = new LinearLayout(this); summaryBox.setOrientation(LinearLayout.VERTICAL);
        summaryBox.setPadding(dp(16),dp(16),dp(16),dp(16));
        summaryBox.setBackground(rounded(th.primary, 18));
        LinearLayout.LayoutParams sLp = new LinearLayout.LayoutParams(-1, -2);
        sLp.setMargins(0,0,0,dp(18));
        root.addView(summaryBox, sLp);

        TextView h = tv("اقساط من", 18, th.text); h.setTypeface(null,1);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(-1,-2);
        hLp.setMargins(dp(2),0,dp(2),dp(8));
        root.addView(h, hLp);

        ScrollView sv = new ScrollView(this);
        sv.setClipToPadding(false);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0,0,0,dp(76));
        sv.addView(list);
        root.addView(sv, new LinearLayout.LayoutParams(-1,0,1));

        outer.addView(root, new FrameLayout.LayoutParams(-1,-1));

        // floating actions: installment calculator above the add-installment button
        TextView calcFab = tv("🧮  ماشین حساب اقساط", 13, th.text);
        calcFab.setGravity(Gravity.CENTER);
        calcFab.setTypeface(null, 1);
        calcFab.setBackground(rounded(th.card, 18));
        calcFab.setElevation(dp(8));
        calcFab.setOnClickListener(v -> startActivity(new Intent(this, InstallmentCalculatorActivity.class)));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(190), dp(46));
        cp.gravity = Gravity.BOTTOM | Gravity.END;
        cp.setMargins(0,0,dp(20),dp(86));
        outer.addView(calcFab, cp);

        TextView fabBtn = tv("＋  تعریف اقساط", 15, th.primaryText);
        fabBtn.setGravity(Gravity.CENTER);
        fabBtn.setTypeface(null,1);
        fabBtn.setBackground(rounded(th.primary, 28));
        fabBtn.setElevation(dp(8));
        fabBtn.setOnClickListener(v -> startActivity(new Intent(this, AddInstallmentActivity.class)));
        FrameLayout.LayoutParams fp = new FrameLayout.LayoutParams(dp(142), dp(58));
        fp.gravity = Gravity.BOTTOM | Gravity.END;
        fp.setMargins(0,0,dp(20),dp(20));
        outer.addView(fabBtn, fp);
        fab = fabBtn;

        // keep content clear of the status bar / navigation bar on edge-to-edge displays
        outer.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets sb = insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(dp(16), sb.top + dp(16), dp(16), dp(12));
            FrameLayout.LayoutParams flp = (FrameLayout.LayoutParams) fab.getLayoutParams();
            flp.bottomMargin = sb.bottom + dp(20);
            fab.setLayoutParams(flp);
            FrameLayout.LayoutParams clp = (FrameLayout.LayoutParams) calcFab.getLayoutParams();
            clp.bottomMargin = sb.bottom + dp(86);
            calcFab.setLayoutParams(clp);
            return insets;
        });

        setContentView(outer);
        refresh();
    }

    @Override protected void onResume() { super.onResume(); if (list != null) refresh(); }

    void summaryRow(String label, String value, boolean strong) {
        LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL);
        TextView l = tv(label, 14, th.primaryText); l.setAlpha(0.85f);
        r.addView(l, new LinearLayout.LayoutParams(0,-2,1));
        TextView v = tv(value, strong ? 16 : 14, th.primaryText);
        if (strong) v.setTypeface(null,1);
        r.addView(v, new LinearLayout.LayoutParams(-2,-2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-2);
        lp.topMargin = dp(6);
        summaryBox.addView(r, lp);
    }

    void refresh() {
        list.removeAllViews();
        List<Installment> all = Store.all(this);

        // --- monthly summary ---
        int[] todayJ = PersianDate.toJalali(Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));

        Map<Long, Long> dueThisMonth = new LinkedHashMap<>(); // installmentId -> amount, deduped
        for (Installment x : all) {
            if (x.isFinished()) continue;
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(x.nextDueMillis());
            int[] j = PersianDate.toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
            if (j[0]==todayJ[0] && j[1]==todayJ[1]) dueThisMonth.put(x.id, x.amount);
        }
        long paidThisMonth = 0;
        for (PaymentLog.Event e : PaymentLog.all(this)) {
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(e.timestamp);
            int[] j = PersianDate.toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
            if (j[0]==todayJ[0] && j[1]==todayJ[1]) {
                paidThisMonth += e.amount;
                dueThisMonth.put(e.installmentId, e.amount); // paid already, but still part of this month's total
            }
        }
        long totalThisMonth = 0; for (long v: dueThisMonth.values()) totalThisMonth += v;
        long remainingThisMonth = Math.max(0, totalThisMonth - paidThisMonth);

        long totalDebt = 0; int openCount = 0;
        Installment next = null;
        for (Installment x: all) if (!x.isFinished()) {
            totalDebt += x.amount * (x.totalCount-x.paidCount);
            openCount++;
            if (next == null || x.nextDueMillis() < next.nextDueMillis()) next = x;
        }

        summaryBox.removeAllViews();
        TextView head = tv("📆  خلاصه‌ی " + PersianDate.MONTH_NAMES[todayJ[1]-1], 15, th.primaryText);
        head.setTypeface(null,1);
        summaryBox.addView(head);
        summaryRow("کل این ماه", money(totalThisMonth), false);
        summaryRow("پرداخت‌شده", money(paidThisMonth), false);
        summaryRow("باقی‌مانده این ماه", money(remainingThisMonth), true);
        summaryRow("قسط بعدی", next == null ? "—" : next.title + "  •  " + next.dueText(), false);
        summaryRow("مانده کل بدهی", money(totalDebt) + "  (" + openCount + " قسط فعال)", false);

        List<Installment> visible = new ArrayList<>();
        for (Installment x: all) if (x.isVisibleOnHome()) visible.add(x);

        if (visible.isEmpty()) {
            String msg = all.isEmpty()
                    ? "هنوز قسطی ثبت نکرده‌ای.\nاز دکمه‌ی + شروع کن."
                    : "الان قسطی نزدیک سررسید نیست 👌";
            TextView e = tv(msg, 16, th.muted);
            e.setGravity(Gravity.CENTER);
            list.addView(e, new LinearLayout.LayoutParams(-1,dp(180)));
            return;
        }

        Collections.sort(visible, (a,b)->Long.compare(a.nextDueMillis(), b.nextDueMillis()));
        for (Installment x: visible) {
            list.addView(CardBuilder.build(this, th, x, new CardBuilder.Actions() {
                @Override public void onPay(Installment y) {
                    confirmPay(y);
                }
                @Override public void onEdit(Installment y) {
                    startActivity(new Intent(MainActivity.this, AddInstallmentActivity.class).putExtra("id", y.id));
                }
                @Override public void onDelete(Installment y) { confirmDelete(y); }
            }));
        }
    }

    void confirmPay(Installment x) {
        int nextNumber = Math.min(x.paidCount + 1, x.totalCount);
        new AlertDialog.Builder(this)
                .setTitle("تأیید پرداخت قسط")
                .setMessage("از پرداخت قسط " + nextNumber + " از " + x.totalCount + " برای «" + x.title + "» به مبلغ " + money(x.amount) + " مطمئنی؟")
                .setPositiveButton("بله، پرداخت شد", (d, w) -> {
                    x.paidCount++;
                    if (x.paidCount >= x.totalCount) x.active = false;
                    Store.upsert(MainActivity.this, x);
                    PaymentLog.record(MainActivity.this, x.id, x.amount);
                    AlarmHelper.schedule(MainActivity.this, x);
                    refresh();
                })
                .setNegativeButton("انصراف", null)
                .show();
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
