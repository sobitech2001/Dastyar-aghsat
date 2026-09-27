package ir.dastyar.eghsat;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Builds one installment row as a card: title, bank badge, progress, due
 * date, plus visible ویرایش/حذف icon buttons. Swiping the card still
 * reveals a quick "✓ پرداخت" action underneath.
 */
public class CardBuilder {

    public interface Actions {
        void onPay(Installment x);
        void onEdit(Installment x);
        void onDelete(Installment x);
    }

    // Only one swiped-open card at a time, across whichever list is on screen.
    private static View openCard = null;

    static int dp(Activity a, float v) { return (int) (v * a.getResources().getDisplayMetrics().density + 0.5f); }

    static GradientDrawable rounded(Activity a, int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(a, radiusDp));
        return g;
    }

    static GradientDrawable roundedStroke(Activity a, int color, int strokeColor, float radiusDp, float strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(a, radiusDp));
        g.setStroke(Math.max(1, dp(a, strokeDp)), strokeColor);
        return g;
    }

    static String money(long n) { return NumberFormat.getInstance(Locale.US).format(n) + " تومان"; }

    public static View build(Activity ctx, Theme th, Installment x, Actions actions) {
        int actionW = dp(ctx, 64);
        int actionsWidth = x.isFinished() ? 0 : actionW; // swipe now only offers the pay shortcut

        FrameLayout wrap = new FrameLayout(ctx);
        LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(-1, -2);
        wrapLp.setMargins(0, dp(ctx, 10), 0, 0);
        wrap.setLayoutParams(wrapLp);

        // Pay shortcut sits pinned to the start edge, hidden behind the face until swiped.
        LinearLayout actionsRow = new LinearLayout(ctx);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setBackground(rounded(ctx, th.card, 14));
        actionsRow.setClipToOutline(true);
        FrameLayout.LayoutParams actionsLp = new FrameLayout.LayoutParams(actionsWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        actionsLp.gravity = Gravity.START;
        wrap.addView(actionsRow, actionsLp);

        if (!x.isFinished()) {
            actionsRow.addView(actionBtn(ctx, "✓ پرداخت", th.success, actionW, v -> { closeOpen(); actions.onPay(x); }));
        }

        View face = buildFace(ctx, th, x, actions);
        wrap.addView(face, new FrameLayout.LayoutParams(-1, -2));

        final float[] startRaw = {0};
        final float[] startTx = {0};
        face.setOnTouchListener((v, event) -> {
            if (actionsWidth == 0) return false; // finished installments: nothing to swipe to
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    startRaw[0] = event.getRawX();
                    startTx[0] = v.getTranslationX();
                    if (openCard != null && openCard != v) closeOpen();
                    return false;
                case MotionEvent.ACTION_MOVE: {
                    float dx = event.getRawX() - startRaw[0];
                    if (Math.abs(dx) > dp(ctx, 6)) {
                        float nt = Math.max(-actionsWidth, Math.min(0, startTx[0] + dx));
                        v.setTranslationX(nt);
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        return true;
                    }
                    return false;
                }
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL: {
                    boolean open = v.getTranslationX() < -actionsWidth / 2f;
                    v.animate().translationX(open ? -actionsWidth : 0).setDuration(160).start();
                    openCard = open ? v : null;
                    return false;
                }
            }
            return false;
        });

        return wrap;
    }

    private static void closeOpen() {
        if (openCard != null) {
            openCard.animate().translationX(0).setDuration(160).start();
            openCard = null;
        }
    }

    private static View actionBtn(Activity ctx, String label, int bg, int w, View.OnClickListener l) {
        TextView t = new TextView(ctx);
        t.setText(label); t.setTextSize(15); t.setTextColor(0xFFFFFFFF);
        t.setTypeface(null, 1);
        t.setGravity(Gravity.CENTER);
        t.setBackgroundColor(bg);
        t.setOnClickListener(l);
        t.setLayoutParams(new LinearLayout.LayoutParams(w, ViewGroup.LayoutParams.MATCH_PARENT));
        return t;
    }

    private static View iconBtn(Activity ctx, String label, int color, View.OnClickListener l) {
        TextView t = new TextView(ctx);
        t.setText(label); t.setTextSize(16); t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        t.setBackground(rounded(ctx, adjustAlpha(color, 0.12f), 10));
        t.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36));
        t.setLayoutParams(lp);
        return t;
    }

    private static View deleteIconBtn(Activity ctx, int color, View.OnClickListener l) {
        ImageView img = new ImageView(ctx);
        img.setImageResource(ir.dastyar.eghsat.R.drawable.ic_delete);
        img.setColorFilter(color);
        img.setScaleType(ImageView.ScaleType.CENTER);
        img.setBackground(rounded(ctx, adjustAlpha(color, 0.12f), 10));
        img.setContentDescription("حذف");
        img.setOnClickListener(l);
        img.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36)));
        return img;
    }

    private static int adjustAlpha(int color, float alpha) {
        return Color.argb((int) (alpha * 255), Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int contrastColor(int bg) {
        double r = Color.red(bg) / 255.0;
        double g = Color.green(bg) / 255.0;
        double b = Color.blue(bg) / 255.0;
        r = r <= 0.03928 ? r / 12.92 : Math.pow((r + 0.055) / 1.055, 2.4);
        g = g <= 0.03928 ? g / 12.92 : Math.pow((g + 0.055) / 1.055, 2.4);
        b = b <= 0.03928 ? b / 12.92 : Math.pow((b + 0.055) / 1.055, 2.4);
        double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        return luminance > 0.45 ? Color.rgb(25,25,30) : Color.WHITE;
    }

    private static int blend(int foreground, int background, float backgroundWeight) {
        float fw = 1f - backgroundWeight;
        return Color.rgb(
                Math.round(Color.red(foreground) * fw + Color.red(background) * backgroundWeight),
                Math.round(Color.green(foreground) * fw + Color.green(background) * backgroundWeight),
                Math.round(Color.blue(foreground) * fw + Color.blue(background) * backgroundWeight)
        );
    }

    private static View buildFace(Activity ctx, Theme th, Installment x, Actions actions) {
        int statusColor = x.isFinished() ? th.success : (x.isDueSoonOrOverdue() ? th.danger : th.primary);
        int accentColor = x.color != 0 ? x.color : statusColor;
        boolean hasCustomColor = x.color != 0;

        LinearLayout row = new LinearLayout(ctx); row.setOrientation(LinearLayout.HORIZONTAL);
        // Keep the installment surface neutral so all text stays readable; the selected color is only the outline.
        row.setBackground(roundedStroke(ctx, th.card, hasCustomColor ? accentColor : th.border, 14, hasCustomColor ? 2 : 1));
        row.setClipToOutline(true);

        LinearLayout inner = new LinearLayout(ctx); inner.setOrientation(LinearLayout.HORIZONTAL);
        inner.setGravity(Gravity.CENTER_VERTICAL);
        inner.setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12));

        Banks.Bank bank = Banks.byId(x.bank);
        if (!bank.id.isEmpty()) {
            TextView badge = new TextView(ctx);
            badge.setText(Banks.initials(bank.name));
            badge.setTextColor(0xFFFFFFFF);
            badge.setTextSize(13);
            badge.setTypeface(null, 1);
            badge.setGravity(Gravity.CENTER);
            GradientDrawable bd = new GradientDrawable();
            bd.setShape(GradientDrawable.OVAL);
            bd.setColor(bank.color);
            badge.setBackground(bd);
            LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(dp(ctx, 30), dp(ctx, 30));
            bLp.setMarginEnd(dp(ctx, 10));
            inner.addView(badge, bLp);
        }

        LinearLayout text = new LinearLayout(ctx); text.setOrientation(LinearLayout.VERTICAL);

        TextView name = new TextView(ctx); name.setText(x.title); name.setTextSize(17);
        name.setTextColor(th.text); name.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        text.addView(name, new LinearLayout.LayoutParams(-2, -2));

        TextView sub = new TextView(ctx);
        sub.setText("قسط " + Math.min(x.paidCount + 1, x.totalCount) + " از " + x.totalCount + "  •  " + money(x.amount));
        sub.setTextSize(15); sub.setTextColor(th.muted); sub.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(-2, -2); subLp.topMargin = dp(ctx, 2);
        text.addView(sub, subLp);

        TextView due = new TextView(ctx);
        due.setText(x.isFinished() ? "✅ تکمیل شده" : "🗓 سررسید: " + x.dueText());
        due.setTextSize(15); due.setTextColor(x.isFinished() ? th.success : statusColor); due.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        LinearLayout.LayoutParams dueLp = new LinearLayout.LayoutParams(-2, -2); dueLp.topMargin = dp(ctx, 2);
        text.addView(due, dueLp);

        inner.addView(text, new LinearLayout.LayoutParams(0, -2, 1));

        // Visible actions for every row. Payment asks for confirmation before it is committed.
        LinearLayout btns = new LinearLayout(ctx); btns.setOrientation(LinearLayout.HORIZONTAL);
        btns.setGravity(Gravity.CENTER_VERTICAL);

        if (!x.isFinished()) {
            TextView pay = new TextView(ctx);
            pay.setText("✓ پرداخت");
            pay.setTextSize(13);
            pay.setTextColor(0xFFFFFFFF);
            pay.setTypeface(null, 1);
            pay.setGravity(Gravity.CENTER);
            pay.setBackground(rounded(ctx, th.success, 10));
            pay.setOnClickListener(v -> actions.onPay(x));
            LinearLayout.LayoutParams payLp = new LinearLayout.LayoutParams(dp(ctx, 64), dp(ctx, 36));
            payLp.setMarginStart(dp(ctx, 6));
            btns.addView(pay, payLp);
        } else {
            TextView paid = new TextView(ctx);
            paid.setText("✓ پرداخت شد");
            paid.setTextSize(12);
            paid.setTextColor(th.success);
            paid.setTypeface(null, 1);
            paid.setGravity(Gravity.CENTER);
            paid.setBackground(rounded(ctx, adjustAlpha(th.success, 0.12f), 10));
            LinearLayout.LayoutParams paidLp = new LinearLayout.LayoutParams(dp(ctx, 72), dp(ctx, 36));
            paidLp.setMarginStart(dp(ctx, 6));
            btns.addView(paid, paidLp);
        }

        LinearLayout.LayoutParams editLp = new LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36));
        editLp.setMarginStart(dp(ctx, 6));
        btns.addView(iconBtn(ctx, "✎", th.primary, v -> actions.onEdit(x)), editLp);
        LinearLayout.LayoutParams delLp = new LinearLayout.LayoutParams(dp(ctx, 36), dp(ctx, 36));
        delLp.setMarginStart(dp(ctx, 6));
        btns.addView(deleteIconBtn(ctx, th.danger, v -> actions.onDelete(x)), delLp);
        inner.addView(btns, new LinearLayout.LayoutParams(-2, -2));

        row.addView(inner, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }
}
