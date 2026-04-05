package com.restaurant.pos.ui.main;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.restaurant.pos.data.db.DatabaseHelper;
import com.restaurant.pos.data.models.*;
import com.restaurant.pos.printing.PrinterManager;
import java.util.*;

public class BillDialog extends Dialog {
    private final String tableNum;
    private final List<OrderItem> items;
    private final DatabaseHelper db;
    private final Runnable onPrinted;
    private LinearLayout itemList;
    private TextView tvSub,tvVat,tvLL,tvUSD;

    public BillDialog(Context ctx,String tableNum,List<OrderItem> items,DatabaseHelper db,Runnable onPrinted){
        super(ctx,android.R.style.Theme_Material_NoTitleBar_Fullscreen);
        this.tableNum=tableNum; this.items=new ArrayList<>(items);
        this.db=db; this.onPrinted=onPrinted;
    }

    @Override
    protected void onCreate(Bundle s){
        super.onCreate(s);
        LinearLayout root=new LinearLayout(getContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#1a1a2e"));
        root.setPadding(dp(10),dp(10),dp(10),dp(10));
        setContentView(root);
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT);

        TextView title=new TextView(getContext());
        title.setText("BILL - TABLE "+tableNum);
        title.setTextColor(Color.WHITE); title.setTextSize(18);
        title.setTypeface(null,android.graphics.Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,0,0,dp(8));
        root.addView(title);

        ScrollView sv=new ScrollView(getContext());
        LinearLayout.LayoutParams svlp=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,0,1f);
        sv.setLayoutParams(svlp);
        itemList=new LinearLayout(getContext());
        itemList.setOrientation(LinearLayout.VERTICAL);
        sv.addView(itemList);
        root.addView(sv);

        tvSub=tv(root,"",13,Color.WHITE);
        tvVat=tv(root,"",13,Color.WHITE);
        tvLL =tv(root,"",20,Color.parseColor("#f1c40f"));
        tvUSD=tv(root,"",20,Color.parseColor("#2ecc71"));
        tvLL.setTypeface(null,android.graphics.Typeface.BOLD);
        tvUSD.setTypeface(null,android.graphics.Typeface.BOLD);

        LinearLayout btns=new LinearLayout(getContext());
        btns.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,dp(52));
        blp.topMargin=dp(8); btns.setLayoutParams(blp); btns.setWeightSum(3f);
        addBtn(btns,"+ Item","#2980b9",()->addItem());
        addBtn(btns,"✓ PRINT","#27ae60",()->doPrint());
        addBtn(btns,"✗ Cancel","#c0392b",this::dismiss);
        root.addView(btns);

        TextView hint=new TextView(getContext());
        hint.setText("Tap item to edit/delete");
        hint.setTextColor(Color.GRAY); hint.setTextSize(10); hint.setGravity(Gravity.CENTER);
        root.addView(hint);

        refresh();
    }

    private void refresh(){
        itemList.removeAllViews();
        long sub=0;
        for(int i=0;i<items.size();i++){
            OrderItem it=items.get(i); long tot=it.subtotal(); sub+=tot;
            LinearLayout row=new LinearLayout(getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0,dp(3),0,dp(3));
            addTv2(row,it.dishNameAr,0.5f,12,Color.WHITE,Gravity.START);
            addTv2(row,String.valueOf(it.quantity),0.15f,12,Color.WHITE,Gravity.CENTER);
            addTv2(row,fmt(tot),0.35f,12,Color.WHITE,Gravity.END);
            final int idx=i;
            row.setOnClickListener(v->editItem(idx));
            itemList.addView(row);
        }
        double vatPct=safeD(db.getSetting("vat_percent","11"),11);
        long vat=(long)(sub*vatPct/100); long tll=sub+vat;
        double dr=safeD(db.getSetting("dollar_rate","90000"),90000);
        double usd=dr>0?tll/dr:0;
        tvSub.setText(String.format(java.util.Locale.getDefault(),"SUB-TOTAL: %,d LL",sub));
        tvVat.setText(String.format(java.util.Locale.getDefault(),"VAT %d%%: %,d LL",(int)vatPct,vat));
        tvLL .setText(String.format(java.util.Locale.getDefault(),"TOTAL LL: %,d",tll));
        tvUSD.setText(String.format(java.util.Locale.getDefault(),"TOTAL $: %.2f",usd));
    }

    private void editItem(int idx){
        OrderItem it=items.get(idx);
        android.app.AlertDialog.Builder b=new android.app.AlertDialog.Builder(getContext());
        b.setTitle(it.dishNameAr);
        LinearLayout c=new LinearLayout(getContext()); c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(16),dp(8),dp(16),dp(8));
        EditText et=new EditText(getContext());
        et.setText(String.valueOf(it.quantity));
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        et.setTextSize(18); et.setGravity(Gravity.CENTER);
        c.addView(et); b.setView(c);
        b.setPositiveButton("Save",(d,w)->{
            try{items.get(idx).quantity=Math.max(1,Integer.parseInt(et.getText().toString()));}catch(Exception e){}
            refresh();
        });
        b.setNegativeButton("Delete",(d,w)->{items.remove(idx);refresh();});
        b.setNeutralButton("Cancel",null);
        b.show();
    }

    private void addItem(){
        List<Dish> dishes=db.getDishes(0,true);
        String[] names=new String[dishes.size()];
        for(int i=0;i<dishes.size();i++)
            names[i]=dishes.get(i).nameAr+"  ("+fmt(dishes.get(i).priceLbp)+" LL)";
        new android.app.AlertDialog.Builder(getContext())
            .setTitle("Add Item")
            .setItems(names,(d,which)->{
                Dish dish=dishes.get(which);
                OrderItem it=new OrderItem();
                it.dishId=dish.id; it.dishNameAr=dish.nameAr; it.dishNameEn=dish.nameEn;
                it.quantity=1; it.priceLbp=dish.priceLbp;
                items.add(it); refresh();
            }).show();
    }

    private void doPrint(){
        String server=db.getSetting("default_server","SERVER");
        Toast.makeText(getContext(),"Printing...",Toast.LENGTH_SHORT).show();
        PrinterManager.printBillAsync(getContext(),tableNum,server,items,(ok,msg)->{
            if(getOwnerActivity()!=null){
                getOwnerActivity().runOnUiThread(()->{
                    Toast.makeText(getContext(),msg,Toast.LENGTH_LONG).show();
                    if(ok){dismiss();onPrinted.run();}
                });
            }
        });
    }

    private TextView tv(LinearLayout p,String t,int sz,int color){
        TextView v=new TextView(getContext());
        v.setText(t); v.setTextColor(color); v.setTextSize(sz); v.setGravity(Gravity.END);
        p.addView(v); return v;
    }
    private void addTv2(LinearLayout p,String t,float w,int sz,int color,int grav){
        TextView v=new TextView(getContext());
        v.setText(t); v.setTextColor(color); v.setTextSize(sz); v.setGravity(grav);
        v.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,w));
        p.addView(v);
    }
    private void addBtn(LinearLayout p,String t,String color,Runnable action){
        Button b=new Button(getContext());
        b.setText(t); b.setBackgroundColor(Color.parseColor(color));
        b.setTextColor(Color.WHITE); b.setTextSize(13);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.MATCH_PARENT,1f);
        lp.setMargins(dp(3),0,dp(3),0); b.setLayoutParams(lp);
        b.setOnClickListener(v->action.run()); p.addView(b);
    }
    private String fmt(long v){return String.format(java.util.Locale.getDefault(),"%,d",v);}
    private double safeD(String s,double d){try{return Double.parseDouble(s.trim());}catch(Exception e){return d;}}
    private int dp(int v){return(int)(v*getContext().getResources().getDisplayMetrics().density);}
}
