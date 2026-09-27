package ir.dastyar.eghsat;

import android.app.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.NumberFormat;
import java.util.*;

public class AddInstallmentActivity extends Activity {
    EditText title, amount, count;
    Spinner bank;
    NumberPicker jYear, jMonth, jDay;
    long editId=-1;
    Theme th;
    int selectedColor = 0;
    List<View> swatches = new ArrayList<>();

    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}

    GradientDrawable rounded(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radiusDp));
        return g;
    }
    GradientDrawable roundedStroke(int color, int strokeColor, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radiusDp)); g.setStroke(dp(1), strokeColor);
        return g;
    }

    TextView label(String s){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(15);t.setTextColor(th.muted);t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        t.setPadding(dp(2),dp(14),dp(2),dp(6));return t;
    }
    EditText field(String hint){
        EditText e=new EditText(this);e.setHint(hint);e.setTextSize(17);e.setSingleLine(true);e.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        e.setTextColor(th.text);e.setHintTextColor(th.muted);
        e.setBackground(roundedStroke(th.card, th.border, 10));
        e.setPadding(dp(14),dp(12),dp(14),dp(12));
        return e;
    }

    LinearLayout.LayoutParams swatchLp(boolean unused) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(32), dp(32));
        lp.setMarginEnd(dp(8));
        return lp;
    }

    View colorSwatch(int color) {
        FrameLayout f = new FrameLayout(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        if (color == 0) { g.setColor(th.card); g.setStroke(dp(2), th.border); }
        else g.setColor(color);
        f.setBackground(g);

        if (color == 0) {
            TextView t = new TextView(this); t.setText("—"); t.setTextColor(th.muted); t.setTextSize(14);
            t.setGravity(Gravity.CENTER);
            f.addView(t, new FrameLayout.LayoutParams(-1,-1));
        }

        View ring = new View(this);
        GradientDrawable rg = new GradientDrawable();
        rg.setShape(GradientDrawable.OVAL);
        rg.setStroke(dp(2), th.text);
        rg.setColor(android.graphics.Color.TRANSPARENT);
        ring.setBackground(rg);
        ring.setTag("ring");
        ring.setVisibility(color == selectedColor ? View.VISIBLE : View.INVISIBLE);
        f.addView(ring, new FrameLayout.LayoutParams(-1,-1));

        f.setTag(color);
        f.setOnClickListener(v -> { selectedColor = color; restyleSwatches(); });
        swatches.add(f);
        return f;
    }

    void restyleSwatches() {
        for (View v : swatches) {
            int c = (Integer) v.getTag();
            View ring = ((FrameLayout) v).findViewWithTag("ring");
            if (ring != null) ring.setVisibility(c == selectedColor ? View.VISIBLE : View.INVISIBLE);
        }
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        th = Theme.get(this);
        getWindow().setStatusBarColor(th.bg);
        editId=getIntent().getLongExtra("id",-1);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(th.bg);
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(20),dp(18),dp(20),dp(24));
        r.setBackground(rounded(th.dark ? th.card : Color.rgb(239,240,243), 18));

        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets sb = insets.getInsets(android.view.WindowInsets.Type.systemBars());
            r.setPadding(dp(20), sb.top + dp(18), dp(20), sb.bottom + dp(24));
            return insets;
        });

        TextView h=new TextView(this);h.setText(editId>0?"ویرایش قسط":"قسط جدید");
        h.setTextSize(21);h.setTypeface(null,1);h.setTextColor(th.text);r.addView(h);

        r.addView(label("عنوان قسط")); title=field("مثلاً وام بانک");r.addView(title);
        r.addView(label("مبلغ هر قسط (تومان)")); amount=field("مثلاً 5,000,000");amount.setInputType(2);r.addView(amount);
        amount.addTextChangedListener(new TextWatcher() {
            boolean editing = false;
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void afterTextChanged(Editable s) {
                if (editing) return;
                editing = true;
                String digits = s.toString().replaceAll("[^0-9]", "");
                if (digits.isEmpty()) { editing = false; return; }
                if (digits.length() > 15) digits = digits.substring(0, 15); // guard against overflow
                String formatted = NumberFormat.getInstance(Locale.US).format(Long.parseLong(digits));
                amount.setText(formatted);
                amount.setSelection(formatted.length());
                editing = false;
            }
        });
        r.addView(label("تعداد کل اقساط")); count=field("مثلاً 24");count.setInputType(2);r.addView(count);

        r.addView(label("بانک"));
        bank = new Spinner(this);
        bank.setAdapter(new BankSpinnerAdapter(this, th));
        bank.setBackground(roundedStroke(th.card, th.border, 10));
        LinearLayout.LayoutParams bankLp = new LinearLayout.LayoutParams(-1, dp(48));
        bankLp.setMargins(0,0,0,dp(4));
        r.addView(bank, bankLp);

        r.addView(label("رنگ قسط"));
        HorizontalScrollView colorScroll = new HorizontalScrollView(this);
        colorScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout colorRow = new LinearLayout(this); colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setPadding(dp(2),dp(3),dp(2),dp(3));
        colorRow.addView(colorSwatch(0), swatchLp(true));
        for (int c : Palette.COLORS) colorRow.addView(colorSwatch(c), swatchLp(false));
        colorScroll.addView(colorRow);
        LinearLayout.LayoutParams colorScrollLp = new LinearLayout.LayoutParams(-1,-2);
        colorScrollLp.setMargins(0,0,0,dp(4));
        r.addView(colorScroll, colorScrollLp);

        r.addView(label("تاریخ اولین سررسید (شمسی)"));

        LinearLayout pickerCard = new LinearLayout(this);
        pickerCard.setBackground(roundedStroke(th.card, th.border, 12));
        pickerCard.setGravity(Gravity.CENTER);
        pickerCard.setPadding(dp(4),dp(4),dp(4),dp(4));

        jDay = new NumberPicker(this); jDay.setMinValue(1); jDay.setMaxValue(31);
        jMonth = new NumberPicker(this); jMonth.setMinValue(1); jMonth.setMaxValue(12);
        jMonth.setDisplayedValues(PersianDate.MONTH_NAMES);
        jYear = new NumberPicker(this); jYear.setMinValue(1380); jYear.setMaxValue(1450);

        int[] today = PersianDate.toJalali(Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        jYear.setValue(today[0]); jMonth.setValue(today[1]); jDay.setValue(today[2]);

        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(0,dp(150),1);
        pickerCard.addView(jDay, pp);
        pickerCard.addView(jMonth, pp);
        pickerCard.addView(jYear, pp);
        LinearLayout.LayoutParams pickerLp = new LinearLayout.LayoutParams(-1,dp(158));
        pickerLp.setMargins(0,0,0,dp(20));
        r.addView(pickerCard, pickerLp);

        TextView save=new TextView(this);save.setText("ذخیره قسط");save.setTextSize(16);
        save.setTextColor(th.primaryText);save.setTypeface(null,1);save.setGravity(Gravity.CENTER);
        save.setBackground(rounded(th.primary, 12));
        save.setOnClickListener(v->save());
        r.addView(save, new LinearLayout.LayoutParams(-1,dp(50)));

        scroll.addView(r);
        setContentView(scroll);
        if(editId>0) load();
    }

    void load(){
        Installment x=Store.find(this,editId); if(x==null)return;
        title.setText(x.title);amount.setText(NumberFormat.getInstance(Locale.US).format(x.amount));count.setText(String.valueOf(x.totalCount));
        for (int i=0;i<Banks.ALL.length;i++) if (Banks.ALL[i].id.equals(x.bank)) { bank.setSelection(i); break; }
        selectedColor = x.color; restyleSwatches();
        Calendar c=Calendar.getInstance();c.setTimeInMillis(x.firstDueMillis);
        int[] j = PersianDate.toJalali(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
        jYear.setValue(j[0]); jMonth.setValue(j[1]); jDay.setValue(Math.min(j[2], PersianDate.daysInJalaliMonth(j[0], j[1])));
    }

    void save(){
        try{
            String t=title.getText().toString().trim();
            long a=Long.parseLong(amount.getText().toString().replaceAll("[^0-9]", ""));
            int c=Integer.parseInt(count.getText().toString().trim());
            if(t.isEmpty()||a<=0||c<=0) throw new Exception();
            Installment x=editId>0?Store.find(this,editId):new Installment();
            if(x==null)x=new Installment();
            x.id=editId>0?editId:System.currentTimeMillis();
            x.title=t;x.amount=a;x.totalCount=c;
            Banks.Bank selectedBank = (Banks.Bank) bank.getSelectedItem();
            x.bank = selectedBank != null ? selectedBank.id : "";
            x.color = selectedColor;

            int jy=jYear.getValue(), jm=jMonth.getValue(), jd=jDay.getValue();
            int maxDay = PersianDate.daysInJalaliMonth(jy, jm);
            if (jd > maxDay) jd = maxDay;
            x.dueDay=jd;
            int[] g = PersianDate.toGregorian(jy, jm, jd);
            Calendar cal=Calendar.getInstance();cal.set(g[0],g[1]-1,g[2],9,0,0);
            x.firstDueMillis=cal.getTimeInMillis();x.active=true;
            Store.upsert(this,x);AlarmHelper.schedule(this,x);finish();
        }catch(Exception e){Toast.makeText(this,"اطلاعات را درست وارد کن.",Toast.LENGTH_SHORT).show();}
    }
}
