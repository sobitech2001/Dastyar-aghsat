package ir.dastyar.eghsat;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;
import java.text.NumberFormat;
import java.util.Locale;

public class InstallmentCalculatorActivity extends BaseActivity {
    Theme th;
    AutoCompleteTextView loanAmount, installmentCount, installmentAmount;
    LinearLayout resultBox;

    TextView tv(String s, float size, int color) { return Ui.tv(this, s, size, color); }
    TextView label(String s) { TextView t=tv(s,14,th.muted); t.setPadding(dp(2),dp(14),dp(2),dp(6)); return t; }

    String digits(String raw) {
        if(raw==null) return "";
        return raw.replace('۰','0').replace('۱','1').replace('۲','2').replace('۳','3').replace('۴','4')
                .replace('۵','5').replace('۶','6').replace('۷','7').replace('۸','8').replace('۹','9')
                .replaceAll("[^0-9]", "");
    }

    AutoCompleteTextView field(String hint, boolean integer) {
        AutoCompleteTextView e=new AutoCompleteTextView(this);
        e.setHint(hint); e.setTextSize(17); e.setSingleLine(true);
        e.setInputType(InputType.TYPE_CLASS_NUMBER);
        e.setFilters(new InputFilter[]{new InputFilter.LengthFilter(22)});
        e.setTextColor(th.text); e.setHintTextColor(th.muted);
        e.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        e.setBackground(roundedStroke(th.card, th.border, 18, 3));
        e.setPadding(dp(14),dp(9),dp(14),dp(9));
        e.setMinHeight(dp(52));
        e.setThreshold(0);
        e.setDropDownVerticalOffset(dp(5));
        e.setDropDownBackgroundDrawable(rounded(th.card, 16));
        e.setSelectAllOnFocus(false);
        return e;
    }

    String moneyOnly(long n) { return NumberFormat.getInstance(Locale.US).format(n); }
    String money(long n) { return moneyOnly(n) + " تومان"; }

    void setupPreset(AutoCompleteTextView field, String[] rawValues, boolean formatThousands) {
        String[] display = new String[rawValues.length];
        for(int i=0;i<rawValues.length;i++) {
            String d=digits(rawValues[i]);
            display[i] = d.isEmpty() ? rawValues[i] : (formatThousands ? moneyOnly(Long.parseLong(d)) : d);
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, display) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v=(TextView)super.getView(position,convertView,parent);
                v.setTextColor(th.text); v.setTextSize(14); v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                v.setPadding(dp(14),dp(8),dp(14),dp(8)); v.setMinHeight(dp(44));
                v.setBackgroundColor(th.card); return v;
            }
        };
        field.setAdapter(adapter);
        attachClearAction(field);
        field.setOnItemClickListener((parent, view, position, id) -> {
            String selected=display[position];
            field.setText(selected); field.setSelection(selected.length());
            hideKeyboard(field); field.dismissDropDown(); field.clearFocus(); updateClearIcon(field);
        });
        field.setOnFocusChangeListener((v,hasFocus)->{
            if(hasFocus) {
                field.postDelayed(() -> { if(field.hasFocus()) field.showDropDown(); }, 80);
            }
        });
    }

    void attachClearAction(AutoCompleteTextView field) {
        field.setCompoundDrawablePadding(dp(6));
        field.setOnTouchListener((v, event) -> {
            if (event.getAction()==android.view.MotionEvent.ACTION_UP && field.getCompoundDrawablesRelative()[2] != null) {
                boolean rtl = field.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL;
                float x=event.getX();
                boolean hit = rtl ? x < dp(52) : x > field.getWidth()-dp(52);
                if (hit) { field.setText(""); field.requestFocus(); field.showDropDown(); android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE); if(imm!=null) imm.showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT); return true; }
            }
            return false;
        });
        field.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s,int st,int c,int a) {}
            @Override public void onTextChanged(CharSequence s,int st,int b,int c) { updateClearIcon(field); }
            @Override public void afterTextChanged(Editable s) {}
        });
        updateClearIcon(field);
        field.setOnClickListener(v -> { field.requestFocus(); field.showDropDown(); android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE); if(imm!=null) imm.showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT); });
    }

    void updateClearIcon(AutoCompleteTextView field) {
        Drawable d=getDrawable(R.drawable.ic_clear);
        d.setTint(th.muted);
        if (field.getText().length()>0) field.setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,d,null);
        else field.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0);
    }

    void makeDropdownOnly(AutoCompleteTextView field) {
        field.setInputType(InputType.TYPE_NULL);
        field.setKeyListener(null);
        field.setFocusable(true);
        field.setFocusableInTouchMode(true);
        field.setOnClickListener(v -> { field.requestFocus(); field.showDropDown(); android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE); if(imm!=null) imm.showSoftInput(field, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT); });
    }

    void formatWhileTyping(AutoCompleteTextView e, boolean thousands) {
        e.addTextChangedListener(new TextWatcher() {
            boolean editing=false;
            @Override public void beforeTextChanged(CharSequence s,int st,int count,int after) {}
            @Override public void onTextChanged(CharSequence s,int st,int before,int count) {}
            @Override public void afterTextChanged(Editable s) {
                if(editing) return;
                String d=digits(s.toString());
                if(d.isEmpty()) return;
                if(d.length()>15) d=d.substring(0,15);
                String out=thousands ? moneyOnly(Long.parseLong(d)) : d;
                if(!out.equals(s.toString())) {
                    editing=true; e.setText(out); e.setSelection(out.length()); editing=false;
                }
            }
        });
    }

    void hideKeyboard(View view) {
        InputMethodManager imm=(InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if(imm!=null) imm.hideSoftInputFromWindow(view.getWindowToken(),0);
    }

    long parseLong(AutoCompleteTextView e) {
        String d=digits(e.getText().toString());
        if(d.isEmpty() || d.length()>15) throw new IllegalArgumentException();
        return Long.parseLong(d);
    }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); th=Theme.get(this); getWindow().setStatusBarColor(th.bg); getWindow().setNavigationBarColor(th.bg);
        ScrollView scroll=new ScrollView(this); scroll.setBackgroundColor(th.bg);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20),dp(18),dp(20),dp(24));
        LinearLayout top=new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView back=tv("‹",22,th.text); back.setGravity(Gravity.CENTER); back.setTypeface(Typeface.DEFAULT,Typeface.BOLD); back.setBackground(roundedStroke(Color.argb(70,Color.red(th.text),Color.green(th.text),Color.blue(th.text)),th.border,16,1)); back.setOnClickListener(v->finish()); top.addView(back,new LinearLayout.LayoutParams(dp(40),dp(40)));
        TextView title=tv("ماشین حساب اقساط",22,th.text); title.setTypeface(null,1); LinearLayout.LayoutParams tl=new LinearLayout.LayoutParams(0,-2,1); tl.setMarginStart(dp(8)); top.addView(title,tl); root.addView(top);
        TextView intro=tv("مبلغ وام، تعداد اقساط و مبلغ هر قسط را وارد کنید.",14,th.muted); intro.setPadding(dp(2),dp(8),dp(2),dp(6)); root.addView(intro);

        root.addView(label("مبلغ وام (تومان)"));
        loanAmount=field("مثلاً 100,000,000",false); formatWhileTyping(loanAmount,true);
        setupPreset(loanAmount,new String[]{"50000000","100000000","200000000","500000000"},true); root.addView(loanAmount,new LinearLayout.LayoutParams(-1,dp(52)));

        root.addView(label("تعداد اقساط"));
        installmentCount=field("مثلاً 24",true);
        setupPreset(installmentCount,new String[]{"6","12","18","24","36","48","60","72","84","96","108","120"},false);
        makeDropdownOnly(installmentCount);
        root.addView(installmentCount,new LinearLayout.LayoutParams(-1,dp(52)));

        root.addView(label("مبلغ هر قسط (تومان)"));
        installmentAmount=field("مثلاً 10,000,000",false); formatWhileTyping(installmentAmount,true);
        setupPreset(installmentAmount,new String[]{"1000000","2000000","5000000","10000000"},true); root.addView(installmentAmount,new LinearLayout.LayoutParams(-1,dp(52)));

        TextView calc=tv("محاسبه",16,th.primaryText); calc.setGravity(Gravity.CENTER); calc.setTypeface(null,1); calc.setBackground(rounded(th.primary,18)); calc.setOnClickListener(v->calculate());
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,dp(52)); clp.setMargins(0,dp(20),0,dp(16)); root.addView(calc,clp);
        resultBox=new LinearLayout(this); resultBox.setOrientation(LinearLayout.VERTICAL); resultBox.setPadding(dp(16),dp(16),dp(16),dp(16)); resultBox.setBackground(rounded(th.card,20)); resultBox.setVisibility(View.GONE); root.addView(resultBox);
        scroll.addView(root);
        scroll.setClipToPadding(false);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets bars = insets.getInsets(android.view.WindowInsets.Type.systemBars());
            root.setPadding(dp(20), bars.top + dp(18), dp(20), bars.bottom + dp(24));
            return insets;
        });
        setContentView(scroll);
    }

    void resultRow(String label,String value,boolean strong){
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        TextView l=tv(label,14,th.muted); TextView v=tv(value,strong?17:15,th.text); if(strong)v.setTypeface(null,1);
        row.addView(l,new LinearLayout.LayoutParams(0,-2,1)); row.addView(v,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,dp(10)); resultBox.addView(row,lp);
    }

    void calculate() {
        try {
            long loan=parseLong(loanAmount), count=parseLong(installmentCount), each=parseLong(installmentAmount);
            if(loan<=0 || count<=0 || each<=0 || count>10000) throw new IllegalArgumentException();
            // Requested validation: an installment may not exceed the loan amount.
            if(loan < each) throw new IllegalArgumentException();
            long total=Math.multiplyExact(count,each);
            if(total < loan) throw new IllegalArgumentException();
            long profit=total-loan; double profitPercent=(profit*100.0)/loan;
            resultBox.removeAllViews();
            TextView head=tv("نتیجه محاسبه",17,th.text); head.setTypeface(null,1); head.setPadding(0,0,0,dp(12)); resultBox.addView(head);
            resultRow("مبلغ وام",money(loan),false); resultRow("کل مبلغ بازپرداخت",money(total),true); resultRow("مبلغ سود / اضافه پرداخت",money(profit),true);
            String pct=String.format(Locale.US,"%.2f٪",profitPercent); TextView percent=tv("درصد سود نسبت به مبلغ وام: "+pct,18,th.primary); percent.setTypeface(null,1); percent.setGravity(Gravity.CENTER); percent.setPadding(dp(10),dp(14),dp(10),dp(14)); percent.setBackground(rounded(Color.argb(31,Color.red(th.primary),Color.green(th.primary),Color.blue(th.primary)),18)); resultBox.addView(percent);
            resultBox.setVisibility(View.VISIBLE);
        } catch(Exception ex) { resultBox.setVisibility(View.GONE); Toast.makeText(this,"لطفا مقادیر صحیح را وارد نمائید",Toast.LENGTH_SHORT).show(); }
    }
}
