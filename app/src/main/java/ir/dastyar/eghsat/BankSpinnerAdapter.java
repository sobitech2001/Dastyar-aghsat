package ir.dastyar.eghsat;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BankSpinnerAdapter extends ArrayAdapter<Banks.Bank> {
    private final Activity ctx;
    private final Theme th;

    public BankSpinnerAdapter(Activity ctx, Theme th) {
        super(ctx, 0, Banks.ALL);
        this.ctx = ctx; this.th = th;
    }

    int dp(float v) { return (int)(v*ctx.getResources().getDisplayMetrics().density+0.5f); }

    private View row(int position, ViewGroup parent, boolean dropdown) {
        Banks.Bank bank = getItem(position);
        LinearLayout row = new LinearLayout(ctx);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(dropdown?12:10), dp(12), dp(dropdown?12:10));
        if (dropdown) row.setBackgroundColor(th.card);

        TextView badge = new TextView(ctx);
        badge.setTextColor(0xFFFFFFFF); badge.setTextSize(12); badge.setTypeface(null,1);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable bd = new GradientDrawable();
        bd.setShape(GradientDrawable.OVAL);
        bd.setColor(bank != null ? bank.color : Banks.NONE.color);
        badge.setBackground(bd);
        badge.setText(bank != null && !bank.id.isEmpty() ? Banks.initials(bank.name) : "–");
        LinearLayout.LayoutParams bLp = new LinearLayout.LayoutParams(dp(26), dp(26));
        bLp.setMarginEnd(dp(10));
        row.addView(badge, bLp);

        TextView name = new TextView(ctx);
        name.setText(bank != null ? bank.name : "");
        name.setTextSize(15);
        name.setTextColor(th.text);
        row.addView(name, new LinearLayout.LayoutParams(-2,-2));

        return row;
    }

    @Override public View getView(int position, View convertView, ViewGroup parent) {
        return row(position, parent, false);
    }

    @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
        return row(position, parent, true);
    }
}
