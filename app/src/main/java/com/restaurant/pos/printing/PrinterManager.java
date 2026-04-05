package com.restaurant.pos.printing;

import android.content.Context;
import android.graphics.*;
import android.util.Log;

import com.restaurant.pos.data.db.DatabaseHelper;
import com.restaurant.pos.data.models.*;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.*;

public class PrinterManager {

    private static final String TAG = "PrinterManager";
    private static final int PAPER_W = 576;
    private static final int PORT = 9100;
    private static final Set<String> NO_MAIN = new HashSet<>(Arrays.asList("BAR", "ARGHILE"));

    public interface PrintCallback {
        void onResult(boolean success, String message);
    }

    public static void printKitchenAsync(Context ctx, String table, String server,
                                          List<OrderItem> items, long orderId, PrintCallback cb) {
        new Thread(() -> {
            DatabaseHelper db = DatabaseHelper.getInstance(ctx);
            int itemSz = safeInt(db.getSetting("kitchen_item_size", "22"), 22);
            String now  = new SimpleDateFormat("dd-MM HH:mm:ss", Locale.getDefault()).format(new Date());
            String time = new SimpleDateFormat("HH:mm:ss",       Locale.getDefault()).format(new Date());

            Map<String, List<OrderItem>> byCategory = new LinkedHashMap<>();
            for (OrderItem it : items)
                byCategory.computeIfAbsent(it.catEn, k -> new ArrayList<>()).add(it);

            StringBuilder log = new StringBuilder();

            for (Map.Entry<String, List<OrderItem>> e : byCategory.entrySet()) {
                String catEn    = e.getKey();
                List<OrderItem> catItems = e.getValue();
                String ip = "";
                for (Category cat : db.getCategories())
                    if (cat.nameEn.equals(catEn)) { ip = cat.printerIp; break; }
                if (ip != null && !ip.trim().isEmpty()) {
                    Bitmap bmp = drawKitchen(table, server, now, time,
                            orderId, items.size(), catItems, itemSz);
                    boolean ok = send(ip.trim(), bmp);
                    if (bmp != null) bmp.recycle();
                    log.append(ok ? "✓ " : "✗ ").append(catEn).append("\n");
                }
            }

            String mainIp = db.getSetting("printer_main_kitchen", "");
            if (mainIp != null && !mainIp.trim().isEmpty()) {
                List<OrderItem> mainItems = new ArrayList<>();
                for (Map.Entry<String, List<OrderItem>> e : byCategory.entrySet())
                    if (!NO_MAIN.contains(e.getKey())) mainItems.addAll(e.getValue());
                if (!mainItems.isEmpty()) {
                    Bitmap bmp = drawKitchen(table, server, now, time,
                            orderId, mainItems.size(), mainItems, itemSz);
                    boolean ok = send(mainIp.trim(), bmp);
                    if (bmp != null) bmp.recycle();
                    log.append(ok ? "✓ MAIN\n" : "✗ MAIN\n");
                }
            }

            String msg = log.length() > 0 ? log.toString().trim() : "No printers configured";
            if (cb != null) cb.onResult(msg.contains("✓"), msg);
        }).start();
    }

    public static void printBillAsync(Context ctx, String table, String server,
                                       List<OrderItem> items, PrintCallback cb) {
        new Thread(() -> {
            DatabaseHelper db = DatabaseHelper.getInstance(ctx);
            String cashierIp = db.getSetting("printer_cashier", "");
            if (cashierIp == null || cashierIp.trim().isEmpty()) {
                if (cb != null) cb.onResult(false, "Cashier printer IP not set!\nGo to Settings ⚙");
                return;
            }
            int itemSz    = safeInt(db.getSetting("bill_item_size","16"), 16);
            double vatPct = safeDouble(db.getSetting("vat_percent","11"), 11);
            double dr     = safeDouble(db.getSetting("dollar_rate","90000"), 90000);
            long sub = 0;
            for (OrderItem it : items) sub += it.subtotal();
            long vat  = (long)(sub * vatPct / 100);
            long tll  = sub + vat;
            double usd = dr > 0 ? tll / dr : 0;

            Bitmap bmp = drawBill(db, table, server, items, sub, vat, tll, usd, (int)vatPct, itemSz);
            boolean ok = send(cashierIp.trim(), bmp);
            if (bmp != null) bmp.recycle();
            if (ok) db.closeTable(table, tll);
            String msg = ok
                ? String.format(Locale.getDefault(), "Bill printed!\n%,d LL  |  $%.2f", tll, usd)
                : "Print failed. Check cashier printer IP.";
            if (cb != null) cb.onResult(ok, msg);
        }).start();
    }

    // ── Draw kitchen receipt ──
    private static Bitmap drawKitchen(String table, String server, String date, String time,
                                       long orderId, int itemCount, List<OrderItem> items, int sz) {
        int lh = sz * 3 + 8;
        int h  = 270 + items.size() * lh + 60;
        Bitmap bmp = Bitmap.createBitmap(PAPER_W, h, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);
        c.drawColor(Color.WHITE);

        int y = 14;
        dC(c,"TABLES", y, 36, true);           y += 52;
        dC(c, server,  y, 22, false);           y += 32;
        dC(c, date+" #"+orderId+" #Itm:"+itemCount, y, 16, false); y += 26;
        dC(c, time,    y, 36, true);            y += 50;
        line(c, y);                              y += 12;
        dC(c, "Guest # 1", y, 20, false);       y += 30;
        line(c, y);                              y += 12;
        dC(c, "TABLE # "+table, y, 44, true);   y += 60;
        line(c, y);                              y += 14;

        Paint p = paint(sz * 2.8f, false);
        for (OrderItem it : items) {
            c.drawText(it.quantity + "  " + it.dishNameEn, 16, y + sz * 2, p);
            y += lh;
        }
        return bmp;
    }

    // ── Draw bill receipt ──
    private static Bitmap drawBill(DatabaseHelper db, String table, String server,
                                    List<OrderItem> items, long sub, long vat, long tll,
                                    double usd, int vatPct, int sz) {
        String rest  = db.getSetting("restaurant_name","Restaurant");
        String addr1 = db.getSetting("restaurant_addr1","");
        String addr2 = db.getSetting("restaurant_addr2","");
        String phone = db.getSetting("restaurant_phone","");
        String date  = new SimpleDateFormat("dd-MM-yyyy",Locale.getDefault()).format(new Date());
        String time  = new SimpleDateFormat("HH:mm",    Locale.getDefault()).format(new Date());

        int lh = sz * 3 + 8;
        int hdrH = 60 + (addr1.isEmpty()?0:28) + (addr2.isEmpty()?0:28)
                + (phone.isEmpty()?0:28) + 120;
        int bodyH = (items.size() + 2) * lh + 20;
        int ftrH  = 200;
        int total = hdrH + bodyH + ftrH;

        Bitmap bmp = Bitmap.createBitmap(PAPER_W, total, Bitmap.Config.ARGB_8888);
        Canvas c   = new Canvas(bmp);
        c.drawColor(Color.WHITE);

        int y = 14;
        dC(c, rest, y, 30, true);               y += 46;
        if (!addr1.isEmpty()) { dC(c,addr1,y,18,false); y+=28; }
        if (!addr2.isEmpty()) { dC(c,addr2,y,18,false); y+=28; }
        if (!phone.isEmpty()) { dC(c,phone,y,18,false); y+=28; }
        dC(c,"TABLES", y, 26, true);             y+=38;
        dC(c, date+"  "+time, y, 18, false);     y+=28;
        dC(c, "TABLE # "+table, y, 18, false);   y+=24;
        dashedLine(c, y);                         y+=14;

        Paint p  = paint(sz * 2.8f, false);
        for (OrderItem it : items) {
            String nm = it.quantity + "  " + it.dishNameEn;
            String pr = fmtLL(it.subtotal());
            c.drawText(nm, 16, y + sz*2, p);
            c.drawText(pr, PAPER_W - p.measureText(pr) - 16, y + sz*2, p);
            y += lh;
        }

        dL(c,"SUB-TOTAL",y,sz); dR(c,fmtLL(sub),y,sz); y+=lh;
        dL(c,"VAT "+vatPct+"%",y,sz); dR(c,fmtLL(vat),y,sz); y+=lh+8;
        dashedLine(c,y);                          y+=14;

        dC(c,"TOTAL LL:  "+fmtLL(tll), y, 34, true); y+=52;
        dC(c,String.format(Locale.getDefault(),"TOTAL $:   %.2f",usd), y, 34, true); y+=52;

        line(c, y);                              y+=14;
        dC(c,"Served by: "+server, y, 18, false); y+=28;
        dC(c,"Thank you", y, 22, true);

        return bmp;
    }

    // ── Helpers ──
    private static void dC(Canvas c, String t, int y, int sp, boolean bold) {
        Paint p = paint(sp * 2.5f, bold);
        c.drawText(t, (PAPER_W - p.measureText(t)) / 2f, y + sp * 2, p);
    }
    private static void dL(Canvas c, String t, int y, int sp) {
        c.drawText(t, 16, y + sp*2, paint(sp*2.5f, false));
    }
    private static void dR(Canvas c, String t, int y, int sp) {
        Paint p = paint(sp*2.5f, false);
        c.drawText(t, PAPER_W - p.measureText(t) - 16, y + sp*2, p);
    }
    private static void line(Canvas c, int y) {
        Paint p = new Paint(); p.setColor(Color.BLACK); p.setStrokeWidth(3f);
        c.drawLine(8, y, PAPER_W-8, y, p);
    }
    private static void dashedLine(Canvas c, int y) {
        Paint p = new Paint(); p.setColor(Color.BLACK); p.setStrokeWidth(2f);
        p.setPathEffect(new DashPathEffect(new float[]{10f,7f},0));
        c.drawLine(8, y, PAPER_W-8, y, p);
    }
    private static Paint paint(float sz, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.BLACK); p.setTextSize(sz);
        p.setTypeface(bold ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        return p;
    }
    private static String fmtLL(long v) {
        return String.format(Locale.getDefault(), "%,d", v);
    }

    // ── TCP send ──
    private static boolean send(String ip, Bitmap bmp) {
        if (bmp == null) return false;
        try {
            byte[] data = toEscPos(bmp);
            Socket s    = new Socket(ip, PORT);
            s.setSoTimeout(6000);
            s.getOutputStream().write(data);
            s.getOutputStream().flush();
            s.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Print err " + ip + ": " + e.getMessage());
            return false;
        }
    }

    private static byte[] toEscPos(Bitmap bmp) throws IOException {
        int w = bmp.getWidth(), h = bmp.getHeight(), wb = (w+7)/8;
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(new byte[]{0x1B,0x40});
        o.write(new byte[]{0x1D,0x76,0x30,0x00});
        o.write(wb&0xFF); o.write((wb>>8)&0xFF);
        o.write(h&0xFF);  o.write((h>>8)&0xFF);
        for (int y=0;y<h;y++) for (int bx=0;bx<wb;bx++) {
            int b=0;
            for (int bit=0;bit<8;bit++) {
                int x=bx*8+bit;
                if (x<w && Color.red(bmp.getPixel(x,y))<128) b|=(0x80>>bit);
            }
            o.write(b);
        }
        o.write(new byte[]{0x1D,0x53});
        return o.toByteArray();
    }

    private static int safeInt(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch(Exception e){ return def; }
    }
    private static double safeDouble(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch(Exception e){ return def; }
    }
}
