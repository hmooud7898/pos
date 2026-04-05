package com.restaurant.pos.ui.main;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import com.restaurant.pos.R;
import com.restaurant.pos.data.db.DatabaseHelper;
import com.restaurant.pos.data.models.*;
import com.restaurant.pos.printing.PrinterManager;
import com.restaurant.pos.ui.settings.SettingsActivity;
import com.restaurant.pos.ui.tables.TablesActivity;
import com.restaurant.pos.ui.reports.ReportsActivity;
import java.util.*;

public class MainActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private String currentTable="";
    private final List<OrderItem> pending=new ArrayList<>();
    private EditText etTable,etServer;
    private LinearLayout tablesBar,orderList;
    private TextView tvTotal,tvTableInfo;

    @Override
    protected void onCreate(Bundle s){
        super.onCreate(s);
        setContentView(R.layout.activity_main);
        db=DatabaseHelper.getInstance(this);
        etTable=findViewById(R.id.et_table);
        etServer=findViewById(R.id.et_server);
        tablesBar=findViewById(R.id.tables_bar);
        orderList=findViewById(R.id.order_list);
        tvTotal=findViewById(R.id.tv_total);
        tvTableInfo=findViewById(R.id.tv_table_info);
        etServer.setText(db.getSetting("default_server","SERVER"));
        findViewById(R.id.btn_go).setOnClickListener(v->goTable());
        etTable.setOnEditorActionListener((v,a,e)->{goTable();return true;});
        findViewById(R.id.btn_settings).setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
        findViewById(R.id.btn_tables).setOnClickListener(v->startActivity(new Intent(this,TablesActivity.class)));
        findViewById(R.id.btn_reports).setOnClickListener(v->startActivity(new Intent(this,ReportsActivity.class)));
        findViewById(R.id.btn_kitchen).setOnClickListener(v->sendKitchen());
        View billBtn=findViewById(R.id.btn_bill);
        billBtn.setOnClickListener(v->openBillPreview());
        billBtn.setOnLongClickListener(v->{printBillDirect();return true;});
        findViewById(R.id.btn_clear).setOnClickListener(v->
            new AlertDialog.Builder(this).setTitle("Clear").setMessage("Clear pending items?")
                .setPositiveButton("Yes",(d,w)->{pending.clear();refreshOrder();})
                .setNegativeButton("No",null).show());
        loadCategoryTabs();
        refreshTablesBar();
    }

    @Override protected void onResume(){super.onResume();refreshTablesBar();refreshOrder();}

    void refreshTablesBar(){
        tablesBar.removeAllViews();
        for(TableSummary t:db.getOpenTables()){
            Button b=new Button(this);
            b.setText(t.tableNum+"\n"+(t.total/1000)+"k");
            b.setBackgroundColor(t.tableNum.equals(currentTable)?Color.parseColor("#e94560"):Color.parseColor("#2c3e50"));
            b.setTextColor(Color.WHITE); b.setTextSize(11);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(dp(72),dp(56));
            lp.setMargins(dp(3),dp(3),dp(3),dp(3)); b.setLayoutParams(lp);
            b.setOnClickListener(v->loadTable(t.tableNum));
            tablesBar.addView(b);
        }
    }

    private void goTable(){
        String t=etTable.getText().toString().trim();
        if(t.isEmpty())return;
        currentTable=t; pending.clear(); refreshOrder();
    }

    void loadTable(String tn){
        currentTable=tn; etTable.setText(tn);
        pending.clear(); refreshOrder(); refreshTablesBar();
    }

    private void loadCategoryTabs(){
        TabLayout tabs=findViewById(R.id.category_tabs);
        tabs.removeAllTabs();
        List<Category> cats=db.getCategories();
        for(Category cat:cats){
            TabLayout.Tab tab=tabs.newTab();
            tab.setText(cat.nameAr); tab.setTag(cat); tabs.addTab(tab);
        }
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener(){
            public void onTabSelected(TabLayout.Tab tab){Category cat=(Category)tab.getTag();if(cat!=null)loadDishes(cat.id);}
            public void onTabUnselected(TabLayout.Tab t){}
            public void onTabReselected(TabLayout.Tab t){}
        });
        if(!cats.isEmpty())loadDishes(cats.get(0).id);
    }

    private void loadDishes(int catId){
        GridView grid=findViewById(R.id.dish_grid);
        List<Dish> dishes=db.getDishes(catId,true);
        DishGridAdapter adapter=new DishGridAdapter(this,dishes,dish->{
            if(currentTable.isEmpty()){toast("Enter table number first!");return;}
            for(OrderItem it:pending){if(it.dishId==dish.id){it.quantity++;refreshOrder();return;}}
            OrderItem ni=new OrderItem();
            ni.dishId=dish.id; ni.dishNameAr=dish.nameAr; ni.dishNameEn=dish.nameEn;
            ni.quantity=1; ni.priceLbp=dish.priceLbp;
            ni.catEn=dish.catEn; ni.catAr=dish.catAr; ni.printerIp=dish.catPrinter;
            pending.add(ni); refreshOrder();
        });
        grid.setAdapter(adapter);
    }

    void refreshOrder(){
        orderList.removeAllViews();
        long total=0;
        tvTableInfo.setText(currentTable.isEmpty()?"No table selected":"TABLE "+currentTable);
        if(!currentTable.isEmpty()){
            List<OrderItem> saved=db.getTableAllItems(currentTable);
            for(OrderItem it:saved){total+=it.subtotal();addRow(it.quantity+"x "+it.dishNameAr,it.subtotal(),Color.parseColor("#aaaaaa"),null);}
            if(!saved.isEmpty()&&!pending.isEmpty()){
                TextView sep=new TextView(this); sep.setText("── NEW ──");
                sep.setTextColor(Color.parseColor("#e67e22")); sep.setTextSize(11);
                sep.setPadding(0,dp(4),0,dp(4)); orderList.addView(sep);
            }
        }
        for(int i=0;i<pending.size();i++){
            OrderItem it=pending.get(i); total+=it.subtotal(); final int idx=i;
            addRow(it.quantity+"x "+it.dishNameAr,it.subtotal(),Color.WHITE,()->{
                if(idx<pending.size()){OrderItem item=pending.get(idx);if(item.quantity>1)item.quantity--;else pending.remove(idx);refreshOrder();}
            });
        }
        double dr=safeD(db.getSetting("dollar_rate","90000"),90000);
        double usd=dr>0?total/dr:0;
        tvTotal.setText(String.format(Locale.getDefault(),"LL: %,d  |  $%.2f",total,usd));
        refreshTablesBar();
    }

    private void addRow(String name,long price,int color,Runnable onTap){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(34));
        rp.setMargins(0,dp(1),0,dp(1)); row.setLayoutParams(rp); row.setPadding(dp(4),0,dp(4),0);
        TextView tvN=new TextView(this); tvN.setText(name); tvN.setTextColor(color); tvN.setTextSize(12);
        tvN.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,0.65f)); row.addView(tvN);
        TextView tvP=new TextView(this); tvP.setText(String.format(Locale.getDefault(),"%,d",price));
        tvP.setTextColor(color); tvP.setTextSize(12); tvP.setGravity(Gravity.END);
        tvP.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,0.35f)); row.addView(tvP);
        if(onTap!=null)row.setOnClickListener(v->onTap.run());
        orderList.addView(row);
    }

    private void sendKitchen(){
        if(pending.isEmpty()){toast("No new items!");return;}
        if(currentTable.isEmpty()){toast("Enter table number!");return;}
        String server=etServer.getText().toString().trim();
        long orderId=db.createOrder(currentTable,server);
        for(OrderItem it:pending)db.addOrderItem(orderId,it.dishId,it.dishNameAr,it.dishNameEn,it.quantity,it.priceLbp,it.catEn,it.catAr,it.printerIp);
        List<OrderItem> toSend=new ArrayList<>(pending);
        pending.clear(); refreshOrder();
        PrinterManager.printKitchenAsync(this,currentTable,server,toSend,orderId,(ok,msg)->runOnUiThread(()->toast("Kitchen: "+msg)));
    }

    private List<OrderItem> getAllItems(){
        List<OrderItem> all=new ArrayList<>(db.getTableAllItems(currentTable));
        all.addAll(pending); return all;
    }

    private void openBillPreview(){
        if(currentTable.isEmpty()){toast("Enter table number!");return;}
        List<OrderItem> items=getAllItems();
        if(items.isEmpty()){toast("No items on this table!");return;}
        new BillDialog(this,currentTable,items,db,()->{pending.clear();currentTable="";etTable.setText("");refreshOrder();}).show();
    }

    private void printBillDirect(){
        if(currentTable.isEmpty())return;
        List<OrderItem> items=getAllItems();
        if(items.isEmpty()){toast("No items!");return;}
        String server=etServer.getText().toString().trim();
        toast("Printing...");
        PrinterManager.printBillAsync(this,currentTable,server,items,(ok,msg)->runOnUiThread(()->{
            toast(msg);
            if(ok){pending.clear();currentTable="";etTable.setText("");refreshOrder();}
        }));
    }

    private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
    private double safeD(String s,double d){try{return Double.parseDouble(s.trim());}catch(Exception e){return d;}}

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String loadTable = intent.getStringExtra("LOAD_TABLE");
        if (loadTable != null && !loadTable.isEmpty()) {
            loadTable(loadTable);
            openBillPreview();
        }
    }
}
// Note: onNewIntent handled by onCreate via FLAG_ACTIVITY_CLEAR_TOP
