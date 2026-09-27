package ir.dastyar.eghsat;

import android.app.Activity;
import android.graphics.Insets;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;

public class CalendarActivity extends Activity {
    Theme th;
    LinearLayout grid, detailBox;
    TextView monthLabel;
    int viewYear, viewMonth; // Jalali
    int todayY, todayM, todayD;
    Map<String, List<Installment>> dueByDay; // "jy-jm-jd" -> installments

    int dp(float v) { return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    String money(long n) { return NumberFormat.getInstance(Locale.US).format(n) + " تومان"; }

    TextView tv(String s, float size, int color) {
        TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        return t;
    }

    GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radiusDp));
        return g;
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.get(this);
        getWindow().setStatusBarColor(th.bg);

        Calendar now = Calendar.getInstance();
        int[] j = PersianDate.toJalali(now.get(Calendar.YEAR), now.get(Calendar.MONTH)+1, now.get(Calendar.DAY_OF_MONTH));
        todayY = j[0]; todayM = j[1]; todayD = j[2];
        viewYear = todayY; viewMonth = todayM;

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
        TextView title = tv("تقویم اقساط", 21, th.text);
        title.setTypeface(null, 1);
        titleCol.addView(title);
        TextView subtitle = tv("سررسیدهای اقساط را روی تقویم ببین", 13, th.muted);
        titleCol.addView(subtitle);
        top.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams topLp = new LinearLayout.LayoutParams(-1,-2);
        topLp.setMargins(0,0,0,dp(12));
        root.addView(top, topLp);

        // month navigation
        LinearLayout nav = new LinearLayout(this); nav.setGravity(Gravity.CENTER_VERTICAL);
        TextView next = navBtn("›", v -> { changeMonth(1); });
        monthLabel = tv("", 17, th.text); monthLabel.setTypeface(null,1); monthLabel.setGravity(Gravity.CENTER);
        monthLabel.setOnClickListener(v -> { viewYear=todayY; viewMonth=todayM; renderMonth(); });
        TextView prev = navBtn("‹", v -> { changeMonth(-1); });
        nav.addView(next, new LinearLayout.LayoutParams(dp(40),dp(40)));
        nav.addView(monthLabel, new LinearLayout.LayoutParams(0,-2,1));
        nav.addView(prev, new LinearLayout.LayoutParams(dp(40),dp(40)));
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(-1,-2);
        navLp.setMargins(0,0,0,dp(10));
        root.addView(nav, navLp);

        // weekday header (Sat..Fri)
        LinearLayout wd = new LinearLayout(this);
        String[] names = {"ش","ی","د","س","چ","پ","ج"};
        for (String n : names) {
            TextView t = tv(n, 12, th.muted); t.setGravity(Gravity.CENTER);
            wd.addView(t, new LinearLayout.LayoutParams(0,-2,1));
        }
        LinearLayout.LayoutParams wdLp = new LinearLayout.LayoutParams(-1,-2);
        wdLp.setMargins(0,0,0,dp(6));
        root.addView(wd, wdLp);

        grid = new LinearLayout(this); grid.setOrientation(LinearLayout.VERTICAL);
        root.addView(grid, new LinearLayout.LayoutParams(-1,-2));

        TextView dh = tv("سررسیدهای این روز", 15, th.text); dh.setTypeface(null,1);
        LinearLayout.LayoutParams dhLp = new LinearLayout.LayoutParams(-1,-2);
        dhLp.setMargins(0,dp(16),0,dp(8));
        root.addView(dh, dhLp);

        ScrollView sv = new ScrollView(this);
        detailBox = new LinearLayout(this); detailBox.setOrientation(LinearLayout.VERTICAL);
        sv.addView(detailBox);
        root.addView(sv, new LinearLayout.LayoutParams(-1,0,1));

        outer.addView(root, new FrameLayout.LayoutParams(-1,-1));
        outer.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets sb = insets.getInsets(WindowInsets.Type.systemBars());
            root.setPadding(dp(16), sb.top + dp(16), dp(16), sb.bottom + dp(12));
            return insets;
        });

        setContentView(outer);
        renderMonth();
    }

    TextView navBtn(String label, View.OnClickListener l) {
        TextView t = tv(label, 20, th.text);
        t.setGravity(Gravity.CENTER);
        t.setBackground(rounded(th.card, 20));
        t.setOnClickListener(l);
        return t;
    }

    void changeMonth(int delta) {
        int m = viewMonth - 1 + delta;
        viewYear += Math.floorDiv(m, 12);
        viewMonth = Math.floorMod(m, 12) + 1;
        renderMonth();
    }

    void renderMonth() {
        monthLabel.setText(PersianDate.MONTH_NAMES[viewMonth-1] + " " + viewYear);

        dueByDay = new HashMap<>();
        for (Installment x : Store.all(this)) {
            if (x.isFinished()) continue;
            Calendar c = Calendar.getInstance(); c.setTimeInMillis(x.nextDueMillis());
            int[] j = PersianDate.toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
            if (j[0]==viewYear && j[1]==viewMonth) {
                String key = j[0]+"-"+j[1]+"-"+j[2];
                dueByDay.computeIfAbsent(key, k -> new ArrayList<>()).add(x);
            }
        }

        grid.removeAllViews();
        int daysInMonth = PersianDate.daysInJalaliMonth(viewYear, viewMonth);
        int[] g1 = PersianDate.toGregorian(viewYear, viewMonth, 1);
        Calendar first = Calendar.getInstance(); first.set(g1[0], g1[1]-1, g1[2], 12, 0, 0);
        int startCol = persianWeekday(first.get(Calendar.DAY_OF_WEEK));

        int day = 1;
        LinearLayout row = null;
        for (int cell = 0; cell < startCol + daysInMonth; cell++) {
            int col = cell % 7;
            if (col == 0) {
                row = new LinearLayout(this);
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, dp(46));
                grid.addView(row, rowLp);
            }
            if (cell < startCol) {
                row.addView(new View(this), new LinearLayout.LayoutParams(0,-1,1));
            } else {
                row.addView(dayCell(day), new LinearLayout.LayoutParams(0,-1,1));
                day++;
            }
        }

        // default detail: today if in view, else first day with something due, else nothing
        if (viewYear==todayY && viewMonth==todayM) showDetails(todayY, todayM, todayD);
        else if (!dueByDay.isEmpty()) {
            String[] parts = dueByDay.keySet().iterator().next().split("-");
            showDetails(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } else { detailBox.removeAllViews(); }
    }

    int persianWeekday(int gregorianDow) {
        switch (gregorianDow) {
            case Calendar.SATURDAY: return 0;
            case Calendar.SUNDAY: return 1;
            case Calendar.MONDAY: return 2;
            case Calendar.TUESDAY: return 3;
            case Calendar.WEDNESDAY: return 4;
            case Calendar.THURSDAY: return 5;
            default: return 6; // FRIDAY
        }
    }

    View dayCell(int d) {
        boolean isToday = (viewYear==todayY && viewMonth==todayM && d==todayD);
        String key = viewYear+"-"+viewMonth+"-"+d;
        boolean hasDue = dueByDay.containsKey(key);

        FrameLayout cell = new FrameLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,-1);
        lp.setMargins(dp(2),dp(2),dp(2),dp(2));
        cell.setLayoutParams(lp);
        if (isToday) cell.setBackground(rounded(th.primary, 10));
        else if (hasDue) cell.setBackground(rounded(th.card, 10));

        TextView num = tv(String.valueOf(d), 14, isToday?th.primaryText:th.text);
        num.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams numLp = new FrameLayout.LayoutParams(-1,-1);
        cell.addView(num, numLp);

        if (hasDue) {
            View dot = new View(this);
            GradientDrawable dd = new GradientDrawable();
            dd.setShape(GradientDrawable.OVAL);
            dd.setColor(isToday ? th.primaryText : th.danger);
            dot.setBackground(dd);
            FrameLayout.LayoutParams dotLp = new FrameLayout.LayoutParams(dp(5),dp(5));
            dotLp.gravity = Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL;
            dotLp.bottomMargin = dp(4);
            cell.addView(dot, dotLp);
        }

        cell.setOnClickListener(v -> showDetails(viewYear, viewMonth, d));
        return cell;
    }

    void showDetails(int jy, int jm, int jd) {
        detailBox.removeAllViews();
        List<Installment> items = dueByDay.get(jy+"-"+jm+"-"+jd);
        if (items == null || items.isEmpty()) {
            TextView e = tv("قسطی سررسید " + jd + " " + PersianDate.MONTH_NAMES[jm-1] + " نیست.", 14, th.muted);
            detailBox.addView(e);
            return;
        }
        for (Installment x : items) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(rounded(th.card, 12));
            row.setClipToOutline(true);
            row.setPadding(0,0,dp(12),0);

            View accent = new View(this);
            accent.setBackgroundColor(x.color != 0 ? x.color : th.primary);
            row.addView(accent, new LinearLayout.LayoutParams(dp(4), dp(52)));

            LinearLayout inner = new LinearLayout(this); inner.setOrientation(LinearLayout.HORIZONTAL);
            inner.setGravity(Gravity.CENTER_VERTICAL);
            inner.setPadding(dp(10),dp(10),0,dp(10));

            Banks.Bank bk = Banks.byId(x.bank);
            if (!bk.id.isEmpty()) {
                TextView badge = tv(Banks.initials(bk.name), 12, 0xFFFFFFFF);
                badge.setTypeface(null,1); badge.setGravity(Gravity.CENTER);
                GradientDrawable bd = new GradientDrawable(); bd.setShape(GradientDrawable.OVAL); bd.setColor(bk.color);
                badge.setBackground(bd);
                LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(dp(26),dp(26));
                bLp.setMarginEnd(dp(10));
                inner.addView(badge, bLp);
            }

            LinearLayout text = new LinearLayout(this); text.setOrientation(LinearLayout.VERTICAL);
            TextView name = tv(x.title, 15, th.text); name.setTypeface(null,1);
            text.addView(name);
            TextView sub = tv(money(x.amount) + "  •  قسط " + Math.min(x.paidCount+1,x.totalCount) + " از " + x.totalCount, 13, th.muted);
            text.addView(sub);
            inner.addView(text, new LinearLayout.LayoutParams(0,-2,1));
            row.addView(inner, new LinearLayout.LayoutParams(0,-2,1));

            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1,-2);
            rowLp.topMargin = dp(8);
            detailBox.addView(row, rowLp);
        }
    }
}
