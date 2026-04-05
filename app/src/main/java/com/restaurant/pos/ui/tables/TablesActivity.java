package com.restaurant.pos.ui.tables;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.restaurant.pos.R;
import com.restaurant.pos.data.db.DatabaseHelper;
import com.restaurant.pos.data.models.*;
import com.restaurant.pos.ui.main.MainActivity;
import java.util.*;
public class TablesActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private String sel=null;
    private LinearLayout tabBar,itemList;
    private TextView tvDetail,tvTotal;
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_tables);
        db=DatabaseHelper.getInstance(this);
        tabBar=findViewById(R.id.tables_bar2);itemList=findViewById(R.id.items_list);
        tvDetail=findViewById(R.id.tv_detail);tvTotal=findViewById(R.id.tv_total2);
        findViewById(R.id.btn_back2).setOnClickListener(v->finish());
        findViewById(R.id.btn_refresh2).setOnClickListener(v->load());
        findViewById(R.id.btn_del_item).setOnClickListener(v->delPicker());
        findViewById(R.id.btn_add_item2).setOnClickListener(v->addPicker());
        findViewById(R.id.btn_bill2).setOnClickListener(v->doBill());
        load();
    }
    @Override protected void onResume(){super.onResume();load();}
    private void load(){
        tabBar.removeAllViews();
        List<TableSummary> tables=db.getOpenTables();
        if(tables.isEmpty()){TextView t=new TextView(this);t.setText("No busy tables");t.setTextColor(Color.GRAY);tabBar.addView(t);return;}
        for(TableSummary t:tables){
            Button b=new Button(this);
            b.setText("TABLE "+t.tableNum+"\n"+(t.total/1000)+"k");
            b.setBackgroundColor(t.tableNum.equals(sel)?Color.parseColor("#e94560"):Color.parseColor("#2c3e50"));
            b.setTextColor(Color.WHITE);b.setTextSize(12);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(120),dp(64));
            lp.setMargins(dp(4),dp(4),dp(4),dp(4));b.setLayoutParams(lp);
            b.setOnClickListener(v->{sel=t.tableNum;load();loadItems();});
            tabBar.addView(b);
        }
    }
    private void loadItems(){
        itemList.removeAllViews();if(sel==null)return;
        tvDetail.setText("TABLE # "+sel);
        List<OrderItem> items=db.getTableAllItems(sel);long total=0;
        for(OrderItem it:items){
            long sub=it.subtotal();total+=sub;
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(4),dp(4),dp(4),dp(4));
            tv(row,it.quantity+"x  "+it.dishNameAr,0.65f,13,Color.WHITE,Gravity.START);
            tv(row,fmt(sub)+" LL",0.35f,13,Color.parseColor("#f1c40f"),Gravity.END);
            itemList.addView(row);
        }
        double dr=safeD(db.getSetting("dollar_rate","90000"),90000);
        tvTotal.setText(String.format(java.util.Locale.getDefault(),"Total: %,d LL  |  $%.2f",total,dr>0?total/dr:0));
    }
    private void delPicker(){
        if(sel==null){t("Select a table first");return;}
        List<OrderItem> items=db.getTableAllItems(sel);if(items.isEmpty()){t("No items");return;}
        String[] names=new String[items.size()];
        for(int i=0;i<items.size();i++)names[i]=items.get(i).quantity+"x "+items.get(i).dishNameAr+" ("+fmt(items.get(i).subtotal())+" LL)";
        new AlertDialog.Builder(this).setTitle("Delete which item?").setItems(names,(d,w)->{db.deleteOrderItem(items.get(w).id);loadItems();load();}).setNegativeButton("Cancel",null).show();
    }
    private void addPicker(){
        if(sel==null){t("Select a table first");return;}
        List<Dish> dishes=db.getDishes(0,true);
        String[] names=new String[dishes.size()];
        for(int i=0;i<dishes.size();i++)names[i]=dishes.get(i).nameAr+"  ("+fmt(dishes.get(i).priceLbp)+" LL)";
        new AlertDialog.Builder(this).setTitle("Add Item").setItems(names,(d,w)->{
            Dish dish=dishes.get(w);
            long oid=db.createOrder(sel,db.getSetting("default_server","SERVER"));
            db.addOrderItem(oid,dish.id,dish.nameAr,dish.nameEn,1,dish.priceLbp,dish.catEn,dish.catAr,dish.catPrinter!=null?dish.catPrinter:"");
            loadItems();load();
        }).setNegativeButton("Cancel",null).show();
    }
    private void doBill(){
        if(sel==null){t("Select a table first");return;}
        Intent i=new Intent(this,MainActivity.class);
        i.putExtra("LOAD_TABLE",sel);i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);finish();
    }
    private void tv(LinearLayout p,String t,float w,int sz,int c,int g){
        TextView v=new TextView(this);v.setText(t);v.setTextColor(c);v.setTextSize(sz);v.setGravity(g);
        v.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,w));p.addView(v);
    }
    private String fmt(long v){return String.format(java.util.Locale.getDefault(),"%,d",v);}
    private double safeD(String s,double d){try{return Double.parseDouble(s.trim());}catch(Exception e){return d;}}
    private void t(String m){Toast.makeText(this,m,Toast.LENGTH_SHORT).show();}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
}
