package ir.dastyar.eghsat;

import android.app.*;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
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

public class AddInstallmentActivity extends BaseActivity {
    AutoCompleteTextView title, amount, count;
    Spinner bank;
    Spinner jYear, jMonth, jDay;
    long editId=-1;
    Theme th;
    int selectedColor = 0;
    List<View> swatches = new ArrayList<>();

    GradientDrawable roundedStroke(int color, int strokeColor, float radiusDp) { return roundedStroke(color, strokeColor, radiusDp, 2); }

    TextView label(String s){
        TextView t=new TextView(this);t.setText(s);t.setTextSize(12);t.setTextColor(th.muted);t.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        t.setPadding(dp(2),dp(8),dp(2),dp(3));return t;
    }
    AutoCompleteTextView field(String hint){
        AutoCompleteTextView e=new AutoCompleteTextView(this);
        e.setHint(hint); e.setTextSize(15); e.setSingleLine(true);
        e.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        e.setTextColor(th.text); e.setHintTextColor(th.muted);
        e.setBackground(roundedStroke(th.card, th.border, 14, 1));
        e.setPadding(dp(12),dp(5),dp(12),dp(5));
        e.setMinHeight(dp(42));
        e.setDropDownBackgroundDrawable(rounded(th.card, 16));
        e.setDropDownVerticalOffset(dp(4));
        e.setThreshold(0);
        e.setOnClickListener(v -> { e.requestFocus(); e.post(() -> { e.showDropDown(); android.view.inputmethod.InputMethodManager imm=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE); if(imm!=null) imm.showSoftInput(e, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT); }); });
        return e;
    }

    void setupPresetField(AutoCompleteTextView field, String[] values) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, values) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v = (TextView) super.getView(position, convertView, parent);
                v.setTextColor(th.text);
                v.setTextSize(14);
                v.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                v.setPadding(dp(14), dp(8), dp(14), dp(8));
                v.setMinHeight(dp(44));
                v.setBackgroundColor(th.card);
                return v;
            }
        };
        field.setAdapter(adapter);
        attachClearAction(field);
        field.setOnItemClickListener((parent, view, position, id) -> {
            hideKeyboard(field);
            field.clearFocus();
            updateClearIcon(field);
        });
    }

    void attachClearAction(AutoCompleteTextView field) {
        field.setCompoundDrawablePadding(dp(6));
        field.setOnTouchListener((v,event)->{
            if(event.getAction()==android.view.MotionEvent.ACTION_UP && field.getCompoundDrawablesRelative()[2]!=null){
                boolean rtl=field.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL; float x=event.getX();
                boolean hit=rtl ? x<dp(52) : x>field.getWidth()-dp(52);
                if(hit){field.setText("");field.requestFocus();field.showDropDown();return true;}
            }
            return false;
        });
        field.addTextChangedListener(new TextWatcher(){
            @Override public void beforeTextChanged(CharSequence s,int a,int b,int c){}
            @Override public void onTextChanged(CharSequence s,int a,int b,int c){updateClearIcon(field);}
            @Override public void afterTextChanged(Editable s){}
        });
        updateClearIcon(field);
    }
    void updateClearIcon(AutoCompleteTextView field){
        Drawable d=getDrawable(R.drawable.ic_clear); d.setTint(th.muted);
        if(field.getText().length()>0) field.setCompoundDrawablesRelativeWithIntrinsicBounds(null,null,d,null);
        else field.setCompoundDrawablesRelativeWithIntrinsicBounds(0,0,0,0);
    }
    void makeDropdownOnly(AutoCompleteTextView field){
        field.setInputType(android.text.InputType.TYPE_NULL); field.setKeyListener(null);
        field.setFocusable(true); field.setFocusableInTouchMode(true);
        field.setOnClickListener(v->{hideKeyboard(field);field.requestFocus();field.showDropDown();});
    }

    void hideKeyboard(View view) {
        android.view.inputmethod.InputMethodManager imm =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    LinearLayout.LayoutParams swatchLp(boolean unused) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(24), dp(24));
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

    Spinner dateSpinner(String[] values) {
        Spinner sp = new Spinner(this);
        String[] display = new String[values.length + 1];
        display[0] = "انتخاب";
        System.arraycopy(values, 0, display, 1, values.length);
        ArrayAdapter<String> a = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, display) {
            @Override public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v=(TextView)super.getView(position,convertView,parent);
                v.setTextColor(position==0 ? th.muted : th.text); v.setTextSize(14); v.setGravity(Gravity.CENTER);
                v.setPadding(dp(6),dp(4),dp(6),dp(4)); v.setMinHeight(dp(38));
                v.setBackgroundColor(Color.TRANSPARENT); return v;
            }
            @Override public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                TextView v=(TextView)super.getDropDownView(position,convertView,parent);
                v.setTextColor(position==0 ? th.muted : th.text); v.setTextSize(14); v.setGravity(Gravity.CENTER);
                v.setPadding(dp(12),dp(8),dp(12),dp(8)); v.setMinHeight(dp(44)); v.setBackgroundColor(th.card); return v;
            }
        };
        sp.setAdapter(a);
        sp.setPopupBackgroundDrawable(rounded(th.card,16));
        sp.setBackground(roundedStroke(th.card,th.border,14,1));
        return sp;
    }

    String[] days(int max) { String[] a=new String[max]; for(int i=0;i<max;i++) a[i]=String.valueOf(i+1); return a; }
    String[] years() { String[] a=new String[71]; for(int i=0;i<a.length;i++) a[i]=String.valueOf(1380+i); return a; }
    void refreshDays() {
        if(jDay==null || jMonth==null || jYear==null) return;
        if(jYear.getSelectedItemPosition()==0 || jMonth.getSelectedItemPosition()==0){
            jDay.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, new String[]{"انتخاب"}));
            return;
        }
        int y=Integer.parseInt((String)jYear.getSelectedItem()); int m=jMonth.getSelectedItemPosition();
        int max=PersianDate.daysInJalaliMonth(y,m); int old=jDay.getSelectedItemPosition();
        String[] vals=days(max); String[] display=new String[vals.length+1]; display[0]="انتخاب"; System.arraycopy(vals,0,display,1,vals.length);
        jDay.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_item,display){
            @Override public View getView(int position,View convertView,android.view.ViewGroup parent){TextView v=(TextView)super.getView(position,convertView,parent);v.setTextColor(position==0?th.muted:th.text);v.setTextSize(14);v.setGravity(Gravity.CENTER);v.setPadding(dp(6),dp(4),dp(6),dp(4));v.setMinHeight(dp(38));return v;}
            @Override public View getDropDownView(int position,View convertView,android.view.ViewGroup parent){TextView v=(TextView)super.getDropDownView(position,convertView,parent);v.setTextColor(position==0?th.muted:th.text);v.setTextSize(14);v.setGravity(Gravity.CENTER);v.setPadding(dp(12),dp(8),dp(12),dp(8));v.setMinHeight(dp(44));v.setBackgroundColor(th.card);return v;}
        });
        jDay.setPopupBackgroundDrawable(rounded(th.card,16)); jDay.setBackground(roundedStroke(th.card,th.border,14,1));
        jDay.setSelection(old==0?0:Math.min(old,max));
    }

    FrameLayout clearableSpinner(Spinner sp, String label){
        FrameLayout box=new FrameLayout(this); box.setClipChildren(false);
        box.addView(sp,new FrameLayout.LayoutParams(-1,-1));
        TextView x=new TextView(this); x.setText("×"); x.setTextSize(16); x.setTypeface(Typeface.DEFAULT,Typeface.BOLD); x.setTextColor(th.muted); x.setGravity(Gravity.CENTER);
        x.setBackground(rounded(Color.argb(45,Color.red(th.text),Color.green(th.text),Color.blue(th.text)),12));
        FrameLayout.LayoutParams xp=new FrameLayout.LayoutParams(dp(26),dp(26),Gravity.LEFT|Gravity.CENTER_VERTICAL); xp.setMargins(dp(6),0,0,0); box.addView(x,xp);
        x.setOnClickListener(v->{sp.setSelection(0); x.setVisibility(View.VISIBLE); sp.performClick();});
        x.setVisibility(sp.getSelectedItemPosition()>0 ? View.VISIBLE : View.INVISIBLE);
        sp.setOnTouchListener((v,event)->{
            if(event.getAction()==android.view.MotionEvent.ACTION_DOWN) x.setVisibility(View.INVISIBLE);
            if(event.getAction()==android.view.MotionEvent.ACTION_UP) x.postDelayed(()->x.setVisibility(sp.getSelectedItemPosition()>0?View.VISIBLE:View.INVISIBLE),320);
            return false;
        });
        return box;
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        th = Theme.get(this);
        getWindow().setStatusBarColor(th.bg);
        editId=getIntent().getLongExtra("id",-1);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(th.bg);
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.VERTICAL);r.setPadding(dp(14),dp(8),dp(14),dp(12));
        r.setBackground(rounded(th.card, 24));

        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            android.graphics.Insets sb = insets.getInsets(android.view.WindowInsets.Type.systemBars());
            r.setPadding(dp(14), sb.top + dp(8), dp(14), sb.bottom + dp(12));
            return insets;
        });

        GradientDrawable headerBg = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{th.primary, blend(th.primary, th.card, 0.28f)});
        headerBg.setCornerRadius(dp(18));
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(8), 0, dp(14), 0);
        header.setBackground(headerBg);
        TextView back= new TextView(this); back.setText("‹"); back.setTextSize(20); back.setTypeface(Typeface.DEFAULT,Typeface.BOLD); back.setTextColor(th.primaryText); back.setGravity(Gravity.CENTER);
        back.setBackground(roundedStroke(Color.argb(45,255,255,255),Color.argb(70,255,255,255),12,1)); back.setOnClickListener(v->finish());
        header.addView(back,new LinearLayout.LayoutParams(dp(34),dp(34)));
        TextView h=new TextView(this);h.setText(editId>0?"ویرایش قسط":"تعریف اقساط");
        h.setTextSize(18);h.setTypeface(null,1);h.setTextColor(th.primaryText);h.setGravity(Gravity.CENTER);
        header.addView(h,new LinearLayout.LayoutParams(0,-2,1));
        header.addView(new View(this),new LinearLayout.LayoutParams(dp(34),dp(34))); // keeps the title centred
        r.addView(header, new LinearLayout.LayoutParams(-1, dp(48)));

        r.addView(label("عنوان قسط"));
        title=field("مثلاً وام بانک");
        setupPresetField(title, new String[]{"وام ازدواج", "وام خودرو", "وام مسکن", "اجاره خانه"});
        r.addView(title,new LinearLayout.LayoutParams(-1,dp(44)));
        r.addView(label("مبلغ هر قسط (تومان)"));
        amount=field("مثلاً 5,000,000"); amount.setInputType(2);
        setupPresetField(amount, new String[]{"2,000,000", "5,000,000", "10,000,000"});
        r.addView(amount,new LinearLayout.LayoutParams(-1,dp(44)));
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
        r.addView(label("تعداد کل اقساط"));
        count=field("مثلاً 24");
        setupPresetField(count, new String[]{"6", "12", "18", "24", "36", "48", "60", "72", "84", "96", "108", "120"});
        makeDropdownOnly(count);
        r.addView(count,new LinearLayout.LayoutParams(-1,dp(44)));

        r.addView(label("بانک"));
        bank = new Spinner(this);
        bank.setAdapter(new BankSpinnerAdapter(this, th));
        bank.setPopupBackgroundDrawable(rounded(th.card,16));
        LinearLayout.LayoutParams bankLp = new LinearLayout.LayoutParams(-1, dp(46)); bankLp.setMargins(0,0,0,dp(2));
        r.addView(clearableSpinner(bank,"بانک"), bankLp);

        r.addView(label("رنگ قسط"));
        HorizontalScrollView colorScroll = new HorizontalScrollView(this);
        colorScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout colorRow = new LinearLayout(this); colorRow.setOrientation(LinearLayout.HORIZONTAL);
        colorRow.setPadding(dp(2),dp(3),dp(2),dp(3));
        colorRow.addView(colorSwatch(0), swatchLp(true));
        for (int c : Palette.COLORS) colorRow.addView(colorSwatch(c), swatchLp(false));
        colorScroll.addView(colorRow);
        LinearLayout.LayoutParams colorScrollLp = new LinearLayout.LayoutParams(-1,-2);
        colorScrollLp.setMargins(0,0,0,dp(2));
        r.addView(colorScroll, colorScrollLp);

        r.addView(label("تاریخ اولین سررسید (شمسی)"));

        LinearLayout pickerCard = new LinearLayout(this);
        pickerCard.setOrientation(LinearLayout.HORIZONTAL);
        pickerCard.setGravity(Gravity.CENTER_VERTICAL);
        pickerCard.setPadding(dp(6), dp(5), dp(6), dp(5));
        pickerCard.setBackground(roundedStroke(th.card, th.border, 16, 1));

        int[] today = PersianDate.toJalali(Calendar.getInstance().get(Calendar.YEAR),
                Calendar.getInstance().get(Calendar.MONTH)+1, Calendar.getInstance().get(Calendar.DAY_OF_MONTH));
        jDay = dateSpinner(days(PersianDate.daysInJalaliMonth(today[0], today[1])));
        jMonth = dateSpinner(PersianDate.MONTH_NAMES);
        jYear = dateSpinner(years());
        jDay.setSelection(Math.max(0, today[2]));
        jMonth.setSelection(Math.max(0, today[1]));
        jYear.setSelection(Math.max(0, today[0]-1380+1));
        jMonth.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { refreshDays(); }
        });
        jYear.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onNothingSelected(android.widget.AdapterView<?> p) {}
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { refreshDays(); }
        });

        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(0, dp(40), 1);
        dateLp.setMargins(dp(3), 0, dp(3), 0);
        pickerCard.addView(clearableSpinner(jDay,"روز"), dateLp);
        pickerCard.addView(clearableSpinner(jMonth,"ماه"), dateLp);
        pickerCard.addView(clearableSpinner(jYear,"سال"), dateLp);
        LinearLayout.LayoutParams pickerLp = new LinearLayout.LayoutParams(-1, dp(52));
        pickerLp.setMargins(0,0,0,dp(12));
        r.addView(pickerCard, pickerLp);

        TextView save=new TextView(this);save.setText("ذخیره قسط");save.setTextSize(15);
        save.setTextColor(th.primaryText);save.setTypeface(null,1);save.setGravity(Gravity.CENTER);
        save.setBackground(rounded(th.primary, 16));
        save.setOnClickListener(v->save());
        r.addView(save, new LinearLayout.LayoutParams(-1,dp(46)));

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
        jYear.setSelection(Math.max(0, j[0]-1380+1));
        jMonth.setSelection(Math.max(0, j[1]));
        refreshDays();
        jDay.setSelection(Math.min(j[2], PersianDate.daysInJalaliMonth(j[0], j[1])));
    }

    void save(){
        try{
            String t=title.getText().toString().trim();
            long a=Long.parseLong(amount.getText().toString().replaceAll("[^0-9]", ""));
            int c=Integer.parseInt(count.getText().toString().trim());
            if(t.isEmpty()||a<=0||c<=0 || jYear.getSelectedItemPosition()==0 || jMonth.getSelectedItemPosition()==0 || jDay.getSelectedItemPosition()==0) throw new Exception();
            Installment x=editId>0?Store.find(this,editId):new Installment();
            if(x==null)x=new Installment();
            x.id=editId>0?editId:System.currentTimeMillis();
            x.title=t;x.amount=a;x.totalCount=c;
            Banks.Bank selectedBank = (Banks.Bank) bank.getSelectedItem();
            x.bank = selectedBank != null ? selectedBank.id : "";
            x.color = selectedColor;

            int jy=Integer.parseInt((String)jYear.getSelectedItem()), jm=jMonth.getSelectedItemPosition(), jd=jDay.getSelectedItemPosition();
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
