package ir.dastyar.eghsat;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

public class MainActivity extends BaseActivity {
    LinearLayout list, root, summaryBox;
    Theme th;
    View fab;
    FrameLayout outer;
    DrawerMenu drawer;
    MonthStrip months;
    final int REQ = 90;
    int homeFilter = 2; // 0 همه، 1 پرداخت شده، 2 پرداخت نشده، 3 معوق شده
    boolean filterCustomized = false;
    boolean keepMonth=false;
    TextView filterStateTop, clearFilterTop;

    TextView tv(String s, float size, int color) { return Ui.tv(this, s, size, color); }
    GradientDrawable stroke(int color,int border,float radiusDp){ return roundedStroke(color,border,radiusDp,1); }

    static final String[] FILTER_NAMES={"همه","پرداخت شده","پرداخت نشده","معوق شده"};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b); th=Theme.get(this); applyBars(); AlarmHelper.rescheduleAll(this); build();
        if (!AppLock.locked) askNotificationPermission();
    }
    @Override protected void onUnlocked(){ askNotificationPermission(); }
    void askNotificationPermission(){
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ);
    }

    void applyBars(){ getWindow().setStatusBarColor(th.bg);getWindow().setNavigationBarColor(th.bg); }

    void build() {
        outer=new FrameLayout(this);outer.setBackgroundColor(th.bg);
        drawer=new DrawerMenu(this,outer,th);
        months=new MonthStrip(this,th,()->refresh());
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(16),dp(16),dp(16),dp(12));

        // Navigation: menu on the left, compact filter button on the right.
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);
        TextView menu=tv("☰",23,th.text);menu.setGravity(Gravity.CENTER);menu.setBackground(rounded(th.card,18));
        menu.setContentDescription("منوی برنامه");menu.setOnClickListener(v->openDrawer());
        top.addView(menu,new LinearLayout.LayoutParams(dp(48),dp(44)));
        Space spacer=new Space(this);top.addView(spacer,new LinearLayout.LayoutParams(0,1,1));
        ImageButton filterBtn=new ImageButton(this);filterBtn.setImageResource(R.drawable.ic_filter);filterBtn.setColorFilter(th.text);filterBtn.setContentDescription("فیلتر اقساط");
        filterBtn.setPadding(dp(10),dp(10),dp(10),dp(10));filterBtn.setBackground(rounded(th.card,18));filterBtn.setOnClickListener(v->showHomeFilter());
        // Filter status sits immediately to the left of the filter icon.
        filterStateTop=tv("",12,th.text); filterStateTop.setTypeface(null,Typeface.BOLD); filterStateTop.setGravity(Gravity.CENTER);
        filterStateTop.setPadding(dp(10),0,dp(10),0);
        filterStateTop.setBackground(stroke(th.card,th.border,14));
        filterStateTop.setOnClickListener(v->showHomeFilter());
        LinearLayout.LayoutParams stateLp=new LinearLayout.LayoutParams(dp(112),dp(38)); stateLp.setMargins(0,0,dp(5),0); top.addView(filterStateTop,stateLp);
        clearFilterTop=tv("× حذف",11,th.primary); clearFilterTop.setTypeface(null,Typeface.BOLD); clearFilterTop.setGravity(Gravity.CENTER);
        clearFilterTop.setVisibility(filterCustomized?View.VISIBLE:View.GONE); clearFilterTop.setOnClickListener(v->{homeFilter=2;filterCustomized=false;refresh();});
        LinearLayout.LayoutParams clearLp=new LinearLayout.LayoutParams(dp(64),dp(38)); clearLp.setMargins(0,0,dp(5),0); top.addView(clearFilterTop,clearLp);
        top.addView(filterBtn,new LinearLayout.LayoutParams(dp(44),dp(44)));
        LinearLayout.LayoutParams topLp=new LinearLayout.LayoutParams(-1,-2);topLp.setMargins(0,0,0,dp(12));root.addView(top,topLp);

        summaryBox=new LinearLayout(this);summaryBox.setOrientation(LinearLayout.VERTICAL);summaryBox.setPadding(dp(10),dp(8),dp(10),dp(8));summaryBox.setBackground(rounded(th.primary,24));
        LinearLayout.LayoutParams sLp=new LinearLayout.LayoutParams(-1,-2);sLp.setMargins(0,0,0,dp(18));root.addView(summaryBox,sLp);
        TextView h=tv("اقساط من",18,th.text);h.setTypeface(null,1);LinearLayout.LayoutParams hLp=new LinearLayout.LayoutParams(-1,-2);hLp.setMargins(dp(2),0,dp(2),dp(8));root.addView(h,hLp);

        ScrollView sv=new ScrollView(this);sv.setClipToPadding(false);list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(0,0,0,dp(76));sv.addView(list);root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        outer.addView(root,new FrameLayout.LayoutParams(-1,-1));

        TextView fabBtn=tv("＋  تعریف اقساط",15,th.primaryText);fabBtn.setGravity(Gravity.CENTER);fabBtn.setTypeface(null,1);fabBtn.setBackground(rounded(th.primary,30));fabBtn.setElevation(dp(8));fabBtn.setOnClickListener(v->startActivity(new Intent(this,AddInstallmentActivity.class)));
        FrameLayout.LayoutParams fp=new FrameLayout.LayoutParams(dp(142),dp(58));fp.gravity=Gravity.BOTTOM|Gravity.RIGHT;fp.setMargins(0,0,dp(20),dp(20));outer.addView(fabBtn,fp);fab=fabBtn;

        outer.setOnApplyWindowInsetsListener((v,insets)->{Insets sb=insets.getInsets(WindowInsets.Type.systemBars());root.setPadding(dp(16),sb.top+dp(12),dp(16),dp(12));FrameLayout.LayoutParams flp=(FrameLayout.LayoutParams)fab.getLayoutParams();flp.bottomMargin=sb.bottom+dp(20);fab.setLayoutParams(flp);return insets;});
        setContentView(outer);refresh();
    }

    @Override protected void onResume(){super.onResume();if(list!=null)refresh();}
    // Leaving the screen (calculator, calendar, other apps...) brings the month strip back to the current month.
    @Override protected void onStop(){ if(!keepMonth&&!isChangingConfigurations()) months.reset(); keepMonth=false; super.onStop(); }
    @Override public void onBackPressed(){if(drawer.isOpen()){drawer.close();return;}super.onBackPressed();}

    void openDrawer(){
        if(drawer.isOpen())return;
        months.reset(); refresh();
        drawer.open();
    }

    /** called by the side menu when a theme button is tapped */
    void onThemePicked(int mode){
        Theme.setMode(this,mode);th=Theme.get(this);applyBars();drawer.close();recreate();
    }

    void summaryRow(String label,String value,boolean strong){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);
        TextView l=tv(label,12,th.primaryText);l.setAlpha(.86f);r.addView(l,new LinearLayout.LayoutParams(0,-2,1));
        TextView v=tv(value,strong?13:12,th.primaryText);if(strong)v.setTypeface(null,1);r.addView(v,new LinearLayout.LayoutParams(-2,-2));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(3);summaryBox.addView(r,lp);
    }

    String monthTitle(int y,int m){ return PersianDate.MONTH_NAMES[m-1]+" "+y; }

    void showHomeFilter(){
        final String[] options=FILTER_NAMES;
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(18),dp(10),dp(18),dp(4));
        TextView title=tv("فیلتر اقساط",18,th.text); title.setTypeface(null,1); title.setGravity(Gravity.RIGHT); box.addView(title,new LinearLayout.LayoutParams(-1,dp(34)));
        RadioGroup group=new RadioGroup(this); group.setOrientation(RadioGroup.VERTICAL);
        for(int i=0;i<options.length;i++){
            final int idx=i; RadioButton rb=new RadioButton(this); rb.setText(options[i]); rb.setTextColor(th.text); rb.setButtonTintList(android.content.res.ColorStateList.valueOf(th.primary)); rb.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); rb.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); rb.setPadding(0,0,0,0); rb.setId(View.generateViewId()); group.addView(rb,new RadioGroup.LayoutParams(-1,dp(46)));
        }
        group.check(group.getChildAt(Math.max(0,Math.min(homeFilter,options.length-1))).getId());
        box.addView(group);
        AlertDialog dialog=new AlertDialog.Builder(this).setView(box).setNegativeButton("انصراف",null).setPositiveButton("نمایش",null).create();
        dialog.setOnShowListener(d->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(th.primary);
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(th.muted);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{homeFilter=group.indexOfChild(group.findViewById(group.getCheckedRadioButtonId())); filterCustomized=true; dialog.dismiss(); refresh();});
        });
        dialog.getWindow(); dialog.show();
        android.view.Window w=dialog.getWindow(); if(w!=null){w.setBackgroundDrawable(rounded(th.card,24));}
    }

    void refresh(){
        list.removeAllViews(); List<Installment> all=Store.all(this);
        Calendar now=Calendar.getInstance();int[] todayJ=PersianDate.toJalali(now.get(Calendar.YEAR),now.get(Calendar.MONTH)+1,now.get(Calendar.DAY_OF_MONTH));
        months.ensureSelected(todayJ);

        summaryBox.removeAllViews();
        if(filterStateTop!=null){
            filterStateTop.setText("فیلتر: "+FILTER_NAMES[Math.max(0,Math.min(homeFilter,3))]);
            filterStateTop.setVisibility(View.VISIBLE);
            if(clearFilterTop!=null) clearFilterTop.setVisibility(filterCustomized?View.VISIBLE:View.GONE);
        }
        months.addTo(summaryBox,todayJ);
        long totalThisMonth=0,paidThisMonth=0,remainingThisMonth=0; int overdueCount=0,monthCount=0;
        for(Installment x:all){
            int idx=x.paymentIndexForMonth(months.year,months.month); if(idx<0) continue;
            monthCount++; totalThisMonth+=x.amount;
            if(idx<x.paidCount) paidThisMonth+=x.amount; else {remainingThisMonth+=x.amount; long due=x.dueMillisForIndex(idx); if(due<System.currentTimeMillis()) overdueCount++;}
        }
        long totalDebt=0;int openCount=0;for(Installment x:all)if(!x.isFinished()){totalDebt+=x.amount*(x.totalCount-x.paidCount);openCount++;}
        TextView head=tv("📆  "+monthTitle(months.year,months.month),14,th.primaryText);head.setTypeface(null,1);head.setGravity(Gravity.RIGHT);summaryBox.addView(head,new LinearLayout.LayoutParams(-1,dp(28)));
        summaryRow("کل اقساط",money(totalThisMonth),false);summaryRow("پرداخت‌شده",money(paidThisMonth),false);summaryRow("باقی‌مانده",money(remainingThisMonth),true);summaryRow("بدهی کل",money(totalDebt)+"  ("+openCount+" فعال)",false);
        List<Installment> visible=new ArrayList<>(); List<Integer> indices=new ArrayList<>();
        boolean selectedCurrent=months.year==todayJ[0]&&months.month==todayJ[1];
        for(Installment x:all){
            int idx=x.paymentIndexForMonth(months.year,months.month); if(idx<0) continue;
            boolean paid=idx<x.paidCount; long due=x.dueMillisForIndex(idx); boolean overdue=!paid&&due<System.currentTimeMillis();
            boolean ok=homeFilter==0 || (homeFilter==1&&paid) || (homeFilter==2&&!paid) || (homeFilter==3&&overdue);
            if(ok){visible.add(x);indices.add(idx);}
        }
        ArrayList<Integer> order=new ArrayList<>();for(int i=0;i<visible.size();i++)order.add(i);
        Collections.sort(order,(a,b)->Long.compare(visible.get(a).dueMillisForIndex(indices.get(a)),visible.get(b).dueMillisForIndex(indices.get(b))));
        if(visible.isEmpty()){
            TextView e=tv(monthCount==0?"در این ماه قسطی برنامه‌ریزی نشده.":"قسطی با این فیلتر پیدا نشد.",16,th.muted);e.setGravity(Gravity.CENTER);list.addView(e,new LinearLayout.LayoutParams(-1,dp(180)));return;
        }
        for(int oi:order){ Installment x=visible.get(oi); int idx=indices.get(oi); boolean canPay=selectedCurrent && idx==x.paidCount && !x.isFinished();
            list.addView(CardBuilder.buildForMonth(this,th,x,new CardBuilder.Actions(){public void onPay(Installment y){confirmPay(y);}public void onEdit(Installment y){keepMonth=true;startActivity(new Intent(MainActivity.this,AddInstallmentActivity.class).putExtra("id",y.id));}public void onDelete(Installment y){confirmDelete(y);}public void onReminderChanged(Installment y){refresh();}},idx,true,canPay));
        }
    }

    void confirmPay(Installment x){ InstallmentActions.confirmPay(this,x,()->refresh()); }
    void confirmDelete(Installment x){ InstallmentActions.confirmDelete(this,x,()->refresh()); }
}
