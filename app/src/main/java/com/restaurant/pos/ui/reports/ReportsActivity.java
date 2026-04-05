package com.restaurant.pos.ui.reports;
import android.graphics.Color;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.restaurant.pos.R;
import com.restaurant.pos.data.db.DatabaseHelper;
import com.restaurant.pos.data.models.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
public class ReportsActivity extends AppCompatActivity {
    private DatabaseHelper db;
    private LinearLayout reportList;
    private TextView tvGrand;
    @Override protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_reports);
        db=DatabaseHelper.getInstance(this);
        reportList=findViewById(R.id.report_list);
        tvGrand=findViewById(R.id.tv_grand);
        findViewById(R.id.btn_back_reports).setOnClickListener(v->finish());
        findViewById(R.id.btn_today_txt).setOnClickListener(v->export("today"));
        findViewById(R.id.btn_month_txt).setOnClickListener(v->export("month"));
        findViewById(R.id.btn_clear_records).setOnClickListener(v->clearMenu());
        loadReport();
    }
    @Override protected void onResume(){super.onResume();loadReport();}
    private void loadReport(){
        reportList.removeAllViews();
        List<ReportItem> rows=db.getDailyReport();
        Map<String,List<ReportItem>> byTable=new LinkedHashMap<>();
        for(ReportItem r:rows)byTable.computeIfAbsent(r.tableNum,k->new ArrayList<>()).add(r);
        long grand=0;
        for(Map.Entry<String,List<ReportItem>> e:byTable.entrySet()){
            TextView th=new TextView(this);
            th.setText("─── TABLE "+e.getKey()+" ───");
            th.setTextColor(Color.parseColor("#4fc3f7"));th.setTextSize(13);th.setTypeface(null,android.graphics.Typeface.BOLD);
            th.setPadding(0,dp(8),0,dp(4));reportList.addView(th);
            for(ReportItem it:e.getValue()){
                grand+=it.subtotal;
                LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(dp(8),dp(3),dp(4),dp(3));
                tv(row,"  "+it.quantity+"x "+it.dishNameEn,0.65f,12,Color.parseColor("#dddddd"),Gravity.START);
                tv(row,fmt(it.subtotal)+" LL",0.35f,12,Color.parseColor("#f1c40f"),Gravity.END);
                reportList.addView(row);
            }
        }
        double dr=safeD(db.getSetting("dollar_rate","90000"),90000);
        tvGrand.setText(String.format(Locale.getDefault(),"Today Total: %,d LL  |  $%.2f",grand,dr>0?grand/dr:0));
    }
    private void export(String period){
        List<ReportItem> rows=period.equals("today")?db.getDailyReport():db.getMonthlyReport();
        String now=new SimpleDateFormat("yyyy-MM-dd_HHmm",Locale.getDefault()).format(new Date());
        String fname="report_"+period+"_"+now+".txt";
        Map<String,List<ReportItem>> byTable=new LinkedHashMap<>();
        for(ReportItem r:rows)byTable.computeIfAbsent(r.tableNum,k->new ArrayList<>()).add(r);
        String rest=db.getSetting("restaurant_name","Restaurant");
        double dr=safeD(db.getSetting("dollar_rate","90000"),90000);
        StringBuilder sb=new StringBuilder();
        sb.append("==================================================\n");
        sb.append("  ").append(rest).append("\n");
        sb.append("  ").append(period.equals("today")?"Daily":"Monthly").append(" Report\n");
        sb.append("  ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date())).append("\n");
        sb.append("==================================================\n");
        long grand=0;
        for(Map.Entry<String,List<ReportItem>> e:byTable.entrySet()){
            sb.append("\n----------------------------------------\n");
            sb.append("  TABLE # ").append(e.getKey()).append("\n");
            sb.append("----------------------------------------\n");
            long ttot=0;
            for(ReportItem it:e.getValue()){
                ttot+=it.subtotal;grand+=it.subtotal;
                sb.append(String.format(Locale.getDefault(),"  %2dx  %-24s %,12d LL\n",it.quantity,it.dishNameEn,it.subtotal));
            }
            sb.append(String.format(Locale.getDefault(),"  TABLE TOTAL: %,d LL  /  $%.2f\n",ttot,dr>0?ttot/dr:0));
        }
        sb.append(String.format(Locale.getDefault(),"\n==================================================\n  GRAND TOTAL LL: %,d\n  GRAND TOTAL $:  %.2f\n==================================================\n",grand,dr>0?grand/dr:0));
        try{
            File dir=new File(getExternalFilesDir(null),"reports");
            dir.mkdirs();
            File file=new File(dir,fname);
            FileWriter fw=new FileWriter(file);fw.write(sb.toString());fw.close();
            Toast.makeText(this,"Saved: "+file.getAbsolutePath(),Toast.LENGTH_LONG).show();
        }catch(Exception ex){Toast.makeText(this,"Error: "+ex.getMessage(),Toast.LENGTH_LONG).show();}
    }
    private void clearMenu(){
        new AlertDialog.Builder(this).setTitle("Clear Records")
            .setItems(new String[]{"Delete > 30 days","Delete > 90 days","Delete ALL closed"},(d,w)->{
                if(w==0){db.clearOldRecords(30);loadReport();Toast.makeText(this,"Done",Toast.LENGTH_SHORT).show();}
                else if(w==1){db.clearOldRecords(90);loadReport();Toast.makeText(this,"Done",Toast.LENGTH_SHORT).show();}
                else new AlertDialog.Builder(this).setTitle("Confirm").setMessage("Delete ALL closed records?")
                    .setPositiveButton("Yes",(d2,w2)->{db.clearAllClosed();loadReport();Toast.makeText(this,"Done",Toast.LENGTH_SHORT).show();})
                    .setNegativeButton("No",null).show();
            }).setNegativeButton("Cancel",null).show();
    }
    private void tv(LinearLayout p,String t,float w,int sz,int c,int g){
        TextView v=new TextView(this);v.setText(t);v.setTextColor(c);v.setTextSize(sz);v.setGravity(g);
        v.setLayoutParams(new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,w));p.addView(v);
    }
    private String fmt(long v){return String.format(Locale.getDefault(),"%,d",v);}
    private double safeD(String s,double d){try{return Double.parseDouble(s.trim());}catch(Exception e){return d;}}
    private int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
}
