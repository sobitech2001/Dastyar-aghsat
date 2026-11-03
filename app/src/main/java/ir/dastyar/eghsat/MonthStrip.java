package ir.dastyar.eghsat;

import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

/**
 * Month picker strip of the home screen. Shows 4 chips at a time
 * (1 past month, current month, 2 future months); the rest scroll.
 */
class MonthStrip {
    private final MainActivity act;
    private final Theme th;
    private final Runnable onChange;
    private HorizontalScrollView scroller;

    /** selected Jalali month; -1 = not chosen yet (means: current month) */
    int year = -1, month = -1;
    /** last horizontal scroll position, -1 = use the default (current month) position */
    int scrollX = -1;

    MonthStrip(MainActivity act, Theme th, Runnable onChange) { this.act = act; this.th = th; this.onChange = onChange; }

    void reset() { year = -1; month = -1; scrollX = -1; }

    void ensureSelected(int[] today) { if (year < 1380 || month < 1) { year = today[0]; month = today[1]; } }

    private int dp(float v) { return Ui.dp(act, v); }

    private static int monthKey(int y, int m) { return y * 12 + (m - 1); }

    private ArrayList<int[]> months(int[] today) {
        ArrayList<int[]> out = new ArrayList<>();
        int current = monthKey(today[0], today[1]);
        int start = current - 6;
        int end = current + 24;
        for (int k = start; k <= end; k++) { int y = k / 12; int m = k % 12 + 1; out.add(new int[]{y, m}); }
        return out;
    }

    void addTo(LinearLayout parent, int[] today) {
        scroller = new HorizontalScrollView(act);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.setFillViewport(false);
        // RTL: past months on the right, future months on the left.
        scroller.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout strip = new LinearLayout(act);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        strip.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        ArrayList<int[]> months = months(today);
        final int n = months.size();
        // 4 chips fill the visible width: 1 past month, current month, 2 future months.
        final int[] itemW = {Math.max(dp(60), (act.getResources().getDisplayMetrics().widthPixels - dp(32) - dp(20)) / 4)};
        final int gap = dp(3);
        int selectedIndex = 0;
        for (int i = 0; i < n; i++) {
            int[] jm = months.get(i);
            if (jm[0] == year && jm[1] == month) selectedIndex = i;
            final int fy = jm[0], fm = jm[1];
            boolean selected = fy == year && fm == month;
            TextView chip = Ui.tv(act, PersianDate.MONTH_NAMES[fm - 1] + "\n" + fy, 11, selected ? th.primaryText : th.text);
            chip.setGravity(Gravity.CENTER); chip.setTypeface(null, Typeface.BOLD);
            chip.setIncludeFontPadding(false); chip.setMaxLines(2); chip.setPadding(dp(2), 0, dp(2), 0);
            chip.setBackground(selected ? Ui.rounded(act, th.primary, 14) : Ui.roundedStroke(act, th.card, th.border, 14, 1));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(itemW[0] - 2 * gap, dp(40));
            lp.setMargins(gap, 0, gap, 0); strip.addView(chip, lp);
            chip.setOnClickListener(v -> { scrollX = scroller.getScrollX(); year = fy; month = fm; onChange.run(); });
        }
        scroller.addView(strip, new HorizontalScrollView.LayoutParams(-2, dp(44)));
        parent.addView(scroller, new LinearLayout.LayoutParams(-1, dp(46)));
        final int si = selectedIndex;
        final HorizontalScrollView sc = scroller;
        sc.post(() -> {
            int w = sc.getWidth(); if (w <= 0) return;
            int iw = w / 4;
            if (Math.abs(iw - itemW[0]) > 1) { // fix up if the real width differs from the estimate
                for (int i = 0; i < strip.getChildCount(); i++) {
                    View c = strip.getChildAt(i); LinearLayout.LayoutParams l = (LinearLayout.LayoutParams) c.getLayoutParams();
                    l.width = iw - 2 * gap; c.setLayoutParams(l);
                }
                itemW[0] = iw;
            }
            sc.post(() -> {
                int max = Math.max(0, n * iw - w);
                // default: previous month at the right edge => [prev][current][+1][+2]
                int target = scrollX >= 0 ? scrollX : max - Math.max(0, si - 1) * iw;
                sc.scrollTo(Math.max(0, Math.min(max, target)), 0);
                sc.setOnScrollChangeListener((v, x, y, ox, oy) -> scrollX = x);
            });
        });
    }
}
