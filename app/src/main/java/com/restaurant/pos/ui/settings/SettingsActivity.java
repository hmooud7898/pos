package com.restaurant.pos.ui.settings;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.tabs.TabLayout;
import com.restaurant.pos.R;
import com.restaurant.pos.data.db.DatabaseHelper;
import com.restaurant.pos.data.models.*;
import java.util.*;
public class SettingsActivity extends AppCompatActivity {
    private DatabaseHelper db;
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_settings);
        db=DatabaseHelper.getInstance(this);
        buildGeneral();buildPrinters();buildDishes();buildPrintSize();
        findViewById(R.id.btn_back_settings).setOnClickListener(v->finish());
    }
    private Map<String,EditText> gFields=new LinkedHashMap<>();
    private void buildGeneral(){
        LinearLayout box=findViewById(R.id.general_box);
        String[][] fields={{"Restaurant Name","restaurant_name"},{"Address 1","restaurant_addr1"},{"Address 2","restaurant_addr2"},{"Phone","restaurant_phone"},{"Server","default_server"},{"Dollar Rate","dollar_rate"},{"VAT %","vat_percent"}};
        for(String[] f:fields){
            TextView lbl=new TextView(this);lbl.setText(f[0]+":");lbl.setTextColor(Color.WHITE);lbl.setTextSize(13);lbl.setPadding(0,dp(6),0,0);box.addView(lbl);
            EditText et=new EditText(this);et.setText(db.getSetting(f[1],""));et.setTextColor(Color.WHITE);et.setHintTextColor(Color.GRAY);et.setBackgroundColor(Color.parseColor("#16213e"));et.setPadding(dp(8),dp(6),dp(8),dp(6));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(6);et.setLayoutParams(lp);
            gFields.put(f[1],et);box.addView(et);
        }
        Button save=new Button(this);save.setText("💾 Save General");save.setBackgroundColor(Color.parseColor("#27ae60"));save.setTextColor(Color.WHITE);
        save.setOnClickListener(v->{for(Map.Entry<String,EditText> e:gFields.entrySet())db.setSetting(e.getKey(),e.getValue().getText().toString());Toast.makeText(this,"Saved!",Toast.LENGTH_SHORT).show();});
        box.addView(save);
    }
    private Map<String,EditText> pFields=new LinkedHashMap<>();
    private Map<Integer,EditText> cFields=new LinkedHashMap<>();
    private void buildPrinters(){
        LinearLayout box=findViewById(R.id.printers_box);
        String[][] fields={{"Main Kitchen","printer_main_kitchen"},{"Cashier","printer_cashier"}};
        for(String[] f:fields){
            TextView lbl=new TextView(this);lbl.setText(f[0]+":");lbl.setTextColor(Color.WHITE);lbl.setTextSize(13);lbl.setPadding(0,dp(6),0,0);box.addView(lbl);
            EditText et=new EditText(this);et.setText(db.getSetting(f[1],""));et.setHint("192.168.1.xxx");et.setTextColor(Color.WHITE);et.setHintTextColor(Color.GRAY);et.setBackgroundColor(Color.parseColor("#16213e"));et.setPadding(dp(8),dp(6),dp(8),dp(6));
            pFields.put(f[1],et);box.addView(et);
        }
        TextView sep=new TextView(this);sep.setText("── Per Category ──");sep.setTextColor(Color.GRAY);sep.setPadding(0,dp(10),0,dp(4));box.addView(sep);
        for(Category cat:db.getCategories()){
            TextView lbl=new TextView(this);lbl.setText(cat.nameAr+":");lbl.setTextColor(Color.WHITE);lbl.setTextSize(13);box.addView(lbl);
            EditText et=new EditText(this);et.setText(cat.printerIp);et.setHint("192.168.1.xxx");et.setTextColor(Color.WHITE);et.setHintTextColor(Color.GRAY);et.setBackgroundColor(Color.parseColor("#16213e"));et.setPadding(dp(8),dp(6),dp(8),dp(6));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT);lp.bottomMargin=dp(6);et.setLayoutParams(lp);
            cFields.put(cat.id,et);box.addView(et);
        }
        Button save=new Button(this);save.setText("💾 Save Printers");save.setBackgroundColor(Color.parseColor("#27ae60"));save.setTextColor(Color.WHITE);
        save.setOnClickListener(v->{
            for(Map.Entry<String,EditText> e:pFields.entrySet())db.setSetting(e.getKey(),e.getValue().getText().toString());
            for(Map.Entry<Integer,EditText> e:cFields.entrySet())db.updateCategoryPrinter(e.getKey(),e.getValue().getText().toString());
            Toast.makeText(this,"Saved!",Toast.LENGTH_SHORT).show();
        });
        box.addView(save);
    }
    private void buildDishes(){
        LinearLayout box=findViewById(R.id.dishes_box);
        buildDishList(box);
        Button add=new Button(this);add.setText("+ Add Dish");add.setBackgroundColor(Color.parseColor("#2980b9"));add.setTextColor(Color.WHITE);
        add.setOnClickListener(v->showDishForm(box,null));
        box.addView(add);
    }
    private void buildDishList(LinearLayout box){
        // Remove all except last button
        while(box.getChildCount()>0)box.removeViewAt(0);
        for(Dish d:db.getDishes(0,false)){
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(42));rp.bottomMargin=dp(3);row.setLayoutParams(rp);
            TextView n=new TextView(this);n.setText(d.nameAr+" / "+d.nameEn);n.setTextColor(Color.WHITE);n.setTextSize(12);
            n.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,0.6f));row.addView(n);
            TextView p=new TextView(this);p.setText(String.format(java.util.Locale.getDefault(),"%,d",d.priceLbp));p.setTextColor(Color.parseColor("#f1c40f"));p.setTextSize(12);p.setGravity(Gravity.END);
            p.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,0.25f));row.addView(p);
            Button edit=new Button(this);edit.setText("✏");edit.setBackgroundColor(Color.parseColor("#e67e22"));edit.setTextColor(Color.WHITE);edit.setTextSize(11);
            LinearLayout.LayoutParams ep=new LinearLayout.LayoutParams(dp(40),LinearLayout.LayoutParams.MATCH_PARENT);ep.leftMargin=dp(4);edit.setLayoutParams(ep);
            edit.setOnClickListener(v->showDishForm(box,d));row.addView(edit);
            Button del=new Button(this);del.setText("🗑");del.setBackgroundColor(Color.parseColor("#c0392b"));del.setTextColor(Color.WHITE);del.setTextSize(11);
            LinearLayout.LayoutParams dp2=new LinearLayout.LayoutParams(dp(40),LinearLayout.LayoutParams.MATCH_PARENT);dp2.leftMargin=dp(3);del.setLayoutParams(dp2);
            del.setOnClickListener(v->{db.deleteDish(d.id);buildDishList(box);});row.addView(del);
            box.addView(row);
        }
    }
    private void showDishForm(LinearLayout box,Dish dish){
        android.app.AlertDialog.Builder b=new android.app.AlertDialog.Builder(this);
        b.setTitle(dish==null?"Add Dish":"Edit Dish");
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(8),dp(16),dp(8));
        EditText etAr=new EditText(this);etAr.setHint("Arabic Name");if(dish!=null)etAr.setText(dish.nameAr);c.addView(etAr);
        EditText etEn=new EditText(this);etEn.setHint("English Name");if(dish!=null)etEn.setText(dish.nameEn);c.addView(etEn);
        EditText etPr=new EditText(this);etPr.setHint("Price LL");etPr.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);if(dish!=null)etPr.setText(String.valueOf(dish.priceLbp));c.addView(etPr);
        List<Category> cats=db.getCategories();
        String[] catNames=new String[cats.size()];for(int i=0;i<cats.size();i++)catNames[i]=cats.get(i).nameAr;
        int[] selCat={dish!=null?0:0};
        if(dish!=null)for(int i=0;i<cats.size();i++)if(cats.get(i).id==dish.categoryId){selCat[0]=i;break;}
        Spinner sp=new Spinner(this);
        ArrayAdapter<String> ad=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,catNames);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);sp.setAdapter(ad);sp.setSelection(selCat[0]);
        c.addView(sp);b.setView(c);
        b.setPositiveButton("Save",(d,w)->{
            int price=0;try{price=Integer.parseInt(etPr.getText().toString());}catch(Exception e){}
            int catId=cats.get(sp.getSelectedItemPosition()).id;
            if(dish==null)db.addDish(etAr.getText().toString(),etEn.getText().toString(),catId,price);
            else db.updateDish(dish.id,etAr.getText().toString(),etEn.getText().toString(),catId,price);
            buildDishList(box);
        });
        b.setNegativeButton("Cancel",null);b.show();
    }
    private void buildPrintSize(){
        LinearLayout box=findViewById(R.id.printsize_box);
        for(String[] f:new String[][]{{"Kitchen Item Size","kitchen_item_size"},{"Bill Item Size","bill_item_size"}}){
            TextView lbl=new TextView(this);lbl.setText(f[0]+":");lbl.setTextColor(Color.WHITE);lbl.setTextSize(14);lbl.setPadding(0,dp(12),0,dp(4));box.addView(lbl);
            LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
            row.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp(54)));
            Button minus=new Button(this);minus.setText("-");minus.setBackgroundColor(Color.parseColor("#c0392b"));minus.setTextColor(Color.WHITE);minus.setTextSize(20);
            minus.setLayoutParams(new LinearLayout.LayoutParams(dp(64),LinearLayout.LayoutParams.MATCH_PARENT));
            TextView val=new TextView(this);val.setText(db.getSetting(f[1],"20"));val.setTextColor(Color.WHITE);val.setTextSize(22);val.setTypeface(null,android.graphics.Typeface.BOLD);val.setGravity(Gravity.CENTER);
            val.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
            Button plus=new Button(this);plus.setText("+");plus.setBackgroundColor(Color.parseColor("#27ae60"));plus.setTextColor(Color.WHITE);plus.setTextSize(20);
            plus.setLayoutParams(new LinearLayout.LayoutParams(dp(64),LinearLayout.LayoutParams.MATCH_PARENT));
            minus.setOnClickListener(v->{int cur=Integer.parseInt(val.getText().toString());if(cur>8){cur--;val.setText(String.valueOf(cur));db.setSetting(f[1],String.valueOf(cur));}});
            plus.setOnClickListener(v->{int cur=Integer.parseInt(val.getText().toString());cur++;val.setText(String.valueOf(cur));db.setSetting(f[1],String.valueOf(cur));});
            row.addView(minus);row.addView(val);row.addView(plus);box.addView(row);
        }
    }
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
}
