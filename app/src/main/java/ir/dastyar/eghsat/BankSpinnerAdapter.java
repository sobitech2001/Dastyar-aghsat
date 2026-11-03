package ir.dastyar.eghsat;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
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

    public BankSpinnerAdapter(Activity ctx, Theme th) { super(ctx, 0, Banks.ALL); this.ctx=ctx; this.th=th; }
    int dp(float v){return Ui.dp(ctx,v);}

    private View row(int position, ViewGroup parent, boolean dropdown) {
        Banks.Bank bank=getItem(position);
        LinearLayout row=new LinearLayout(ctx); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10),dp(dropdown?7:5),dp(10),dp(dropdown?7:5));
        if(dropdown) row.setBackground(rounded(th.card,16));

        LinearLayout logo=new LinearLayout(ctx); logo.setGravity(Gravity.CENTER); logo.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg=new GradientDrawable(); bg.setShape(GradientDrawable.RECTANGLE); bg.setCornerRadius(dp(11)); bg.setColor(bank!=null?bank.color:Banks.NONE.color); bg.setStroke(dp(1), 0x55FFFFFF); logo.setBackground(bg);
        TextView mark=new TextView(ctx); mark.setText(Banks.mark(bank)); mark.setTextColor(Color.WHITE); mark.setTextSize(dropdown?11:10); mark.setTypeface(Typeface.create("sans-serif",Typeface.BOLD)); mark.setGravity(Gravity.CENTER); mark.setIncludeFontPadding(false); logo.addView(mark,new LinearLayout.LayoutParams(-1,-1));
        LinearLayout.LayoutParams logoLp=new LinearLayout.LayoutParams(dp(dropdown?42:34),dp(dropdown?32:28)); logoLp.setMarginEnd(dp(10)); row.addView(logo,logoLp);

        TextView name=new TextView(ctx); name.setText(bank!=null?bank.name:""); name.setTextSize(dropdown?14:13); name.setTextColor(th.text); name.setTypeface(Typeface.DEFAULT,Typeface.BOLD); name.setGravity(Gravity.CENTER_VERTICAL); row.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        return row;
    }
    GradientDrawable rounded(int color,float radius){return Ui.rounded(ctx,color,radius);}
    @Override public View getView(int position,View convertView,ViewGroup parent){return row(position,parent,false);}
    @Override public View getDropDownView(int position,View convertView,ViewGroup parent){return row(position,parent,true);}
}
