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
        default void onReminderChanged(Installment x) {}
    }

    // Only one swiped-open card at a time, across whichever list is on screen.
    private static View openCard = null;

    static int dp(Activity a, float v) { return Ui.dp(a, v); }
    static GradientDrawable rounded(Activity a, int color, float radiusDp) { return Ui.rounded(a, color, radiusDp); }
    static GradientDrawable roundedStroke(Activity a, int color, int strokeColor, float radiusDp, float strokeDp) { return Ui.roundedStroke(a, color, strokeColor, radiusDp, strokeDp); }
    static String money(long n) { return Ui.money(n); }

    public static View build(Activity ctx, Theme th, Installment x, Actions actions) { return build(ctx, th, x, actions, true); }

    public static View build(Activity ctx, Theme th, Installment x, Actions actions, boolean showPayment) {
        return buildInternal(ctx, th, x, actions, showPayment, x.paidCount, showPayment);
    }

    public static View buildForMonth(Activity ctx, Theme th, Installment x, Actions actions, int displayIndex, boolean showPayment, boolean canPay) {
        return buildInternal(ctx, th, x, actions, showPayment, displayIndex, canPay);
    }

    private static View buildInternal(Activity ctx, Theme th, Installment x, Actions actions, boolean showPayment, int displayIndex, boolean canPay) {
        boolean displayPaid = displayIndex < x.paidCount;
        int actionW = dp(ctx, 64);
        int actionsWidth = (!showPayment || x.isFinished() || !canPay || displayPaid) ? 0 : actionW; // swipe now only offers the pay shortcut

        FrameLayout wrap = new FrameLayout(ctx);
        LinearLayout.LayoutParams wrapLp = new LinearLayout.LayoutParams(-1, -2);
        wrapLp.setMargins(0, dp(ctx, 10), 0, 0);
        wrap.setLayoutParams(wrapLp);

        // Pay shortcut sits pinned to the start edge, hidden behind the face until swiped.
        LinearLayout actionsRow = new LinearLayout(ctx);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setBackground(rounded(ctx, th.card, 18));
        actionsRow.setClipToOutline(true);
        FrameLayout.LayoutParams actionsLp = new FrameLayout.LayoutParams(actionsWidth, ViewGroup.LayoutParams.MATCH_PARENT);
        actionsLp.gravity = Gravity.START;
        wrap.addView(actionsRow, actionsLp);

        if (showPayment && canPay && !x.isFinished() && !displayPaid) {
            actionsRow.addView(actionBtn(ctx, "✓ پرداخت", th.success, actionW, v -> { closeOpen(); actions.onPay(x); }));
        }

        View face = buildFace(ctx, th, x, actions, showPayment, displayIndex, canPay);
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
        t.setBackground(rounded(ctx, adjustAlpha(color, 0.10f), 14));
        t.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40));
        t.setLayoutParams(lp);
        return t;
    }

    private static View deleteIconBtn(Activity ctx, int color, View.OnClickListener l) {
        ImageView img = new ImageView(ctx);
        img.setImageResource(ir.dastyar.eghsat.R.drawable.ic_delete);
        img.setColorFilter(color);
        img.setScaleType(ImageView.ScaleType.CENTER);
        img.setBackground(rounded(ctx, adjustAlpha(color, 0.10f), 14));
        img.setContentDescription("حذف");
        img.setOnClickListener(l);
        img.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, 40), dp(ctx, 40)));
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

    private static int blend(int foreground, int background, float backgroundWeight) { return Ui.blend(foreground, background, backgroundWeight); }

    private static View buildFace(Activity ctx, Theme th, Installment x, Actions actions, boolean showPayment, int displayIndex, boolean canPay) {
        long displayDue = displayIndex >= 0 && displayIndex < x.totalCount ? x.dueMillisForIndex(displayIndex) : x.nextDueMillis();
        boolean displayPaid = displayIndex < x.paidCount;
        boolean displayFinished = displayIndex >= x.totalCount;
        boolean displayOverdue = !displayPaid && displayDue < System.currentTimeMillis();
        int statusColor = displayPaid || x.isFinished() ? th.success : (displayOverdue ? th.danger : th.primary);
        int accentColor = x.color != 0 ? x.color : statusColor;
        boolean hasCustomColor = x.color != 0;

        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        int rowSurface = hasCustomColor ? blend(accentColor, th.card, 0.93f) : th.card;
        int rowBorder = hasCustomColor ? adjustAlpha(accentColor, 0.22f) : th.border;
        row.setBackground(roundedStroke(ctx, rowSurface, rowBorder, 20, hasCustomColor ? 1.2f : 1f));
        row.setClipToOutline(true);
        row.setPadding(dp(ctx, 10), dp(ctx, 14), dp(ctx, 10), dp(ctx, 14));

        // Right side: amount is the visual focus of the installment card.
        LinearLayout amountBox = new LinearLayout(ctx);
        amountBox.setOrientation(LinearLayout.VERTICAL);
        amountBox.setGravity(Gravity.CENTER);
        TextView amount = new TextView(ctx);
        amount.setText(money(x.amount));
        amount.setTextSize(17);
        amount.setTextColor(hasCustomColor ? rowTextColor(th, accentColor) : th.text);
        amount.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        amount.setGravity(Gravity.CENTER);
        amount.setSingleLine(true);
        amount.setIncludeFontPadding(false);
        amountBox.addView(amount, new LinearLayout.LayoutParams(-1, -2));
        TextView toman = new TextView(ctx);
        toman.setText("مبلغ قسط"); toman.setTextSize(9); toman.setTextColor(th.muted); toman.setGravity(Gravity.CENTER); toman.setSingleLine(true);
        amountBox.addView(toman, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams amountLp = new LinearLayout.LayoutParams(dp(ctx, 118), -2);
        amountLp.setMarginStart(dp(ctx, 8));
        row.addView(amountBox, amountLp);

        // Center: title and due date are kept compact and on one line.
        LinearLayout info = new LinearLayout(ctx); info.setOrientation(LinearLayout.VERTICAL); info.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = new TextView(ctx);
        name.setText(x.title); name.setTextSize(14); name.setTextColor(th.text); name.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        name.setSingleLine(true); name.setEllipsize(android.text.TextUtils.TruncateAt.END); name.setGravity(Gravity.RIGHT);
        info.addView(name, new LinearLayout.LayoutParams(-1, dp(ctx, 28)));

        TextView due = new TextView(ctx);
        String dueText;
        if (displayPaid) dueText = "✓ پرداخت شده";
        else if (displayFinished) dueText = "✓ تکمیل شده";
        else {
            java.util.Calendar dc = java.util.Calendar.getInstance(); dc.setTimeInMillis(displayDue);
            int[] dj = PersianDate.toJalali(dc.get(java.util.Calendar.YEAR), dc.get(java.util.Calendar.MONTH)+1, dc.get(java.util.Calendar.DAY_OF_MONTH));
            dueText = (displayOverdue ? "⚠ " : "🗓 ") + String.format(java.util.Locale.US,"%04d/%02d/%02d",dj[0],dj[1],dj[2]);
        }
        due.setText(dueText); due.setTextSize(10); due.setTextColor(displayPaid ? th.success : (displayOverdue ? th.danger : th.muted));
        due.setSingleLine(true); due.setEllipsize(android.text.TextUtils.TruncateAt.END); due.setGravity(Gravity.RIGHT);
        info.addView(due, new LinearLayout.LayoutParams(-1, dp(ctx, 26)));
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(0, -2, 1f);
        row.addView(info, infoLp);

        // Left side: installment number, then compact actions.
        LinearLayout left = new LinearLayout(ctx); left.setOrientation(LinearLayout.VERTICAL); left.setGravity(Gravity.CENTER);
        TextView index = new TextView(ctx);
        index.setText("قسط " + Math.min(displayIndex + 1, x.totalCount) + " از " + x.totalCount);
        index.setTextSize(11); index.setTextColor(th.muted); index.setGravity(Gravity.CENTER); index.setSingleLine(true);
        left.addView(index, new LinearLayout.LayoutParams(dp(ctx, 82), dp(ctx, 28)));

        LinearLayout btns = new LinearLayout(ctx); btns.setOrientation(LinearLayout.HORIZONTAL); btns.setGravity(Gravity.CENTER);
        int iconSize=30;
        if (showPayment && canPay && !x.isFinished() && !displayPaid) {
            btns.addView(iconBtn(ctx, "✓", th.success, v -> actions.onPay(x)), new LinearLayout.LayoutParams(dp(ctx, iconSize),dp(ctx,iconSize)));
        }
        btns.addView(reminderButton(ctx, th, x, actions, iconSize, hasCustomColor ? rowTextColor(th, accentColor) : th.primary));
        btns.addView(iconBtn(ctx, "✎", hasCustomColor ? rowTextColor(th, accentColor) : th.primary, v -> actions.onEdit(x)), new LinearLayout.LayoutParams(dp(ctx, iconSize),dp(ctx,iconSize)));
        btns.addView(deleteIconBtn(ctx, hasCustomColor ? rowTextColor(th, accentColor) : th.danger, v -> actions.onDelete(x)), new LinearLayout.LayoutParams(dp(ctx, iconSize),dp(ctx,iconSize)));
        left.addView(btns, new LinearLayout.LayoutParams(dp(ctx, iconSize*4+6), dp(ctx, iconSize)));
        row.addView(left, new LinearLayout.LayoutParams(dp(ctx, 122), -2));
        return row;
    }


    private static View reminderButton(Activity ctx, Theme th, Installment x, Actions actions, int size, int neutralColor) {
        TextView t = new TextView(ctx);
        t.setText("🔔");
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER);
        t.setTypeface(null, Typeface.BOLD);
        t.setContentDescription(x.reminderMode == 0 ? "یادآور خاموش" : "یادآور فعال");
        int offBg = adjustAlpha(neutralColor, 0.10f);
        int activeBg = adjustAlpha(th.success, 0.20f);
        int activeIcon = th.success;
        t.setTextColor(x.reminderMode == 0 ? neutralColor : activeIcon);
        t.setBackground(rounded(ctx, x.reminderMode == 0 ? offBg : activeBg, 14));
        t.setOnClickListener(v -> {
            if (x.reminderMode == 0) {
                ReminderDialog.show(ctx, th, x, () -> actions.onReminderChanged(x));
                return;
            }
            // Red is the explicit "tap to disable" state. It is temporary and
            // becomes neutral after confirmation, so the normal active state stays green.
            t.setTextColor(th.danger);
            t.setBackground(rounded(ctx, adjustAlpha(th.danger, 0.20f), 14));
            android.app.AlertDialog confirm = new android.app.AlertDialog.Builder(ctx)
                    .setTitle("خاموش کردن یادآور")
                    .setMessage("یادآور «" + x.title + "» غیرفعال شود؟")
                    .setPositiveButton("غیرفعال شود", (d,w) -> {
                        x.reminderMode = 0;
                        Store.upsert(ctx, x);
                        AlarmHelper.cancel(ctx, x.id);
                        actions.onReminderChanged(x);
                    })
                    .setNegativeButton("انصراف", null)
                    .create();
            confirm.setOnDismissListener(d -> {
                if (x.reminderMode != 0) {
                    t.setTextColor(activeIcon);
                    t.setBackground(rounded(ctx, activeBg, 14));
                } else {
                    t.setTextColor(neutralColor);
                    t.setBackground(rounded(ctx, offBg, 14));
                    t.setContentDescription("یادآور خاموش");
                }
            });
            confirm.show();
        });
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(ctx, size), dp(ctx, size)));
        return t;
    }
    private static int rowTextColor(Theme th, int accent) {
        return blend(accent, th.text, 0.35f);
    }

}
