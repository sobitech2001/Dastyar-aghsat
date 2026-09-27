package ir.dastyar.eghsat;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.NumberFormat;
import java.util.Locale;

public class InstallmentCalculatorActivity extends Activity {
    Theme th;
    EditText loanAmount, installmentCount, installmentAmount;
    LinearLayout resultBox;

    int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    GradientDrawable roundedStroke(int color, int strokeColor, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), strokeColor);
        return g;
    }

    TextView tv(String s, float size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    TextView label(String s) {
        TextView t = tv(s, 14, th.muted);
        t.setPadding(dp(2), dp(14), dp(2), dp(6));
        return t;
    }

    EditText field(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setInputType(2);
        e.setTextColor(th.text);
        e.setHintTextColor(th.muted);
        e.setBackground(roundedStroke(th.card, th.border, 10));
        e.setPadding(dp(14), dp(12), dp(14), dp(12));
        return e;
    }

    String money(long n) {
        return NumberFormat.getInstance(Locale.US).format(n) + " تومان";
    }

    void addNumberFormatter(EditText field) {
        field.addTextChangedListener(new TextWatcher() {
            boolean editing = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (editing) return;
                String digits = s.toString().replaceAll("[^0-9]", "");
                if (digits.isEmpty()) return;
                if (digits.length() > 15) digits = digits.substring(0, 15);
                try {
                    String formatted = NumberFormat.getInstance(Locale.US).format(Long.parseLong(digits));
                    editing = true;
                    field.setText(formatted);
                    field.setSelection(formatted.length());
                    editing = false;
                } catch (Exception ignored) {}
            }
        });
    }

    long value(EditText e) {
        String digits = e.getText().toString().replaceAll("[^0-9]", "");
        if (digits.isEmpty()) throw new IllegalArgumentException();
        return Long.parseLong(digits);
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        th = Theme.get(this);
        getWindow().setStatusBarColor(th.bg);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(th.bg);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(18), dp(20), dp(24));

        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets sb = insets.getInsets(android.view.WindowInsets.Type.systemBars());
            root.setPadding(dp(20), sb.top + dp(18), dp(20), sb.bottom + dp(24));
            return insets;
        });

        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back = tv("←", 22, th.text);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        top.addView(back, new LinearLayout.LayoutParams(dp(44), dp(44)));

        TextView title = tv("ماشین حساب اقساط", 22, th.text);
        title.setTypeface(null, 1);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(0, -2, 1);
        titleLp.setMarginStart(dp(8));
        top.addView(title, titleLp);
        root.addView(top);

        TextView intro = tv("با ۳ عدد، مبلغ کل بازپرداخت و درصد سود را حساب کن.", 14, th.muted);
        intro.setPadding(dp(2), dp(8), dp(2), dp(6));
        root.addView(intro);

        root.addView(label("مبلغ وام (تومان)"));
        loanAmount = field("مثلاً 100,000,000");
        addNumberFormatter(loanAmount);
        root.addView(loanAmount);

        root.addView(label("تعداد اقساط"));
        installmentCount = field("مثلاً 24");
        root.addView(installmentCount);

        root.addView(label("مبلغ هر قسط (تومان)"));
        installmentAmount = field("مثلاً 10,000,000");
        addNumberFormatter(installmentAmount);
        root.addView(installmentAmount);

        TextView calc = tv("محاسبه", 16, th.primaryText);
        calc.setGravity(Gravity.CENTER);
        calc.setTypeface(null, 1);
        calc.setBackground(rounded(th.primary, 12));
        calc.setOnClickListener(v -> calculate());
        LinearLayout.LayoutParams calcLp = new LinearLayout.LayoutParams(-1, dp(50));
        calcLp.setMargins(0, dp(20), 0, dp(16));
        root.addView(calc, calcLp);

        resultBox = new LinearLayout(this);
        resultBox.setOrientation(LinearLayout.VERTICAL);
        resultBox.setPadding(dp(16), dp(16), dp(16), dp(16));
        resultBox.setBackground(rounded(th.card, 16));
        resultBox.setVisibility(View.GONE);
        root.addView(resultBox);

        scroll.addView(root);
        setContentView(scroll);
    }

    void resultRow(String label, String value, boolean strong) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView l = tv(label, 14, th.muted);
        TextView v = tv(value, strong ? 17 : 15, th.text);
        if (strong) v.setTypeface(null, 1);
        row.addView(l, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(v, new LinearLayout.LayoutParams(-2, -2));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(0, 0, 0, dp(10));
        resultBox.addView(row, lp);
    }

    void calculate() {
        try {
            long loan = value(loanAmount);
            long count = value(installmentCount);
            long each = value(installmentAmount);

            if (loan <= 0 || count <= 0 || each <= 0) throw new IllegalArgumentException();

            long total;
            try {
                total = Math.multiplyExact(count, each);
            } catch (ArithmeticException e) {
                throw new IllegalArgumentException();
            }

            long profit = total - loan;
            double profitPercent = (profit * 100.0) / loan;

            resultBox.removeAllViews();

            TextView head = tv("نتیجه محاسبه", 17, th.text);
            head.setTypeface(null, 1);
            head.setPadding(0, 0, 0, dp(12));
            resultBox.addView(head);

            resultRow("مبلغ وام", money(loan), false);
            resultRow("کل مبلغ بازپرداخت", money(total), true);
            resultRow("مبلغ سود / اضافه پرداخت", money(profit), true);

            String percentText = String.format(Locale.US, "%.2f٪", profitPercent);
            TextView percent = tv("درصد سود نسبت به مبلغ وام: " + percentText, 18, th.primary);
            percent.setTypeface(null, 1);
            percent.setGravity(Gravity.CENTER);
            percent.setPadding(dp(10), dp(14), dp(10), dp(14));
            percent.setBackground(rounded(adjustAlpha(th.primary, 0.12f), 12));
            resultBox.addView(percent);

            if (profit < 0) {
                TextView note = tv("مبلغ کل اقساط از اصل وام کمتر است؛ درصد نمایش‌داده‌شده منفی است.", 12, th.muted);
                note.setGravity(Gravity.CENTER);
                note.setPadding(0, dp(10), 0, 0);
                resultBox.addView(note);
            }
            resultBox.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            Toast.makeText(this, "هر ۳ فیلد را با عدد معتبر پر کن.", Toast.LENGTH_SHORT).show();
        }
    }

    int adjustAlpha(int color, float alpha) {
        return Color.argb((int)(alpha * 255), Color.red(color), Color.green(color), Color.blue(color));
    }
}
