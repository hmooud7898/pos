package com.restaurant.pos.data.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.restaurant.pos.data.models.*;

import java.util.*;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "pos.db";
    private static final int DB_VERSION = 1;
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context ctx) {
        if (instance == null)
            instance = new DatabaseHelper(ctx.getApplicationContext());
        return instance;
    }

    private DatabaseHelper(Context ctx) {
        super(ctx, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS settings(key TEXT PRIMARY KEY, value TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS categories(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name_ar TEXT NOT NULL," +
                "name_en TEXT NOT NULL UNIQUE," +
                "printer_ip TEXT DEFAULT '')");
        db.execSQL("CREATE TABLE IF NOT EXISTS dishes(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name_ar TEXT NOT NULL," +
                "name_en TEXT NOT NULL," +
                "category_id INTEGER," +
                "price_lbp INTEGER DEFAULT 0," +
                "active INTEGER DEFAULT 1)");
        db.execSQL("CREATE TABLE IF NOT EXISTS orders(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "table_num TEXT NOT NULL," +
                "server_name TEXT NOT NULL," +
                "status TEXT DEFAULT 'open'," +
                "created_at TEXT DEFAULT (datetime('now','localtime'))," +
                "total_lbp INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS order_items(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "order_id INTEGER," +
                "dish_id INTEGER," +
                "dish_name_ar TEXT," +
                "dish_name_en TEXT," +
                "quantity INTEGER DEFAULT 1," +
                "price_lbp INTEGER DEFAULT 0," +
                "cat_en TEXT DEFAULT ''," +
                "cat_ar TEXT DEFAULT ''," +
                "printer_ip TEXT DEFAULT ''," +
                "note TEXT DEFAULT '')");

        String[][] defs = {
            {"restaurant_name","My Restaurant"},{"restaurant_addr1",""},
            {"restaurant_addr2",""},{"restaurant_phone",""},
            {"default_server","SERVER"},{"dollar_rate","90000"},
            {"vat_percent","11"},{"printer_main_kitchen",""},
            {"printer_cashier",""},{"kitchen_item_size","22"},
            {"bill_item_size","16"}
        };
        for (String[] kv : defs)
            db.execSQL("INSERT OR IGNORE INTO settings(key,value) VALUES(?,?)",
                    new Object[]{kv[0], kv[1]});

        String[][] cats = {
            {"فرن / مناقيش","FORN"},{"مشاوي","MASHAWI"},{"سخن","SAKHN"},
            {"سلطات","SALATA"},{"سندويش","SANDWICH"},{"بيتزا","PIZZA"},
            {"بار","BAR"},{"أراكيل","ARGHILE"}
        };
        for (String[] cat : cats)
            db.execSQL("INSERT OR IGNORE INTO categories(name_ar,name_en) VALUES(?,?)",
                    new Object[]{cat[0], cat[1]});
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n) {}

    // Settings
    public String getSetting(String key, String def) {
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery("SELECT value FROM settings WHERE key=?",
                     new String[]{key})) {
            return c.moveToFirst() ? c.getString(0) : def;
        } catch (Exception e) { return def; }
    }

    public void setSetting(String key, String value) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("key", key); cv.put("value", value);
        db.insertWithOnConflict("settings", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Categories
    public List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery("SELECT * FROM categories ORDER BY id", null)) {
            while (c.moveToNext()) list.add(Category.fromCursor(c));
        }
        return list;
    }

    public void updateCategoryPrinter(int catId, String ip) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("printer_ip", ip);
        db.update("categories", cv, "id=?", new String[]{String.valueOf(catId)});
    }

    // Dishes
    public List<Dish> getDishes(int categoryId, boolean activeOnly) {
        StringBuilder where = new StringBuilder(activeOnly ? "d.active=1" : "1=1");
        if (categoryId > 0) where.append(" AND d.category_id=").append(categoryId);
        List<Dish> list = new ArrayList<>();
        String q = "SELECT d.*,c.name_ar as cat_ar,c.name_en as cat_en," +
                "c.printer_ip as cat_printer FROM dishes d " +
                "JOIN categories c ON d.category_id=c.id WHERE " + where +
                " ORDER BY d.name_ar";
        try (SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery(q, null)) {
            while (c.moveToNext()) list.add(Dish.fromCursor(c));
        }
        return list;
    }

    public void addDish(String nameAr, String nameEn, int catId, int price) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name_ar", nameAr); cv.put("name_en", nameEn);
        cv.put("category_id", catId); cv.put("price_lbp", price);
        db.insert("dishes", null, cv);
    }

    public void updateDish(int id, String nameAr, String nameEn, int catId, int price) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name_ar", nameAr); cv.put("name_en", nameEn);
        cv.put("category_id", catId); cv.put("price_lbp", price);
        db.update("dishes", cv, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteDish(int id) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("active", 0);
        db.update("dishes", cv, "id=?", new String[]{String.valueOf(id)});
    }

    // Orders
    public long createOrder(String tableNum, String server) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("table_num", tableNum); cv.put("server_name", server);
        return db.insert("orders", null, cv);
    }

    public void addOrderItem(long orderId, int dishId, String nameAr, String nameEn,
                              int qty, int price, String catEn, String catAr, String printerIp) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("order_id", orderId); cv.put("dish_id", dishId);
        cv.put("dish_name_ar", nameAr); cv.put("dish_name_en", nameEn);
        cv.put("quantity", qty); cv.put("price_lbp", price);
        cv.put("cat_en", catEn); cv.put("cat_ar", catAr);
        cv.put("printer_ip", printerIp); cv.put("note", "");
        db.insert("order_items", null, cv);
    }

    public List<OrderItem> getTableAllItems(String tableNum) {
        List<OrderItem> list = new ArrayList<>();
        String q = "SELECT oi.* FROM order_items oi JOIN orders o ON oi.order_id=o.id " +
                "WHERE o.table_num=? AND o.status='open' ORDER BY o.created_at,oi.id";
        try (SQLiteDatabase db = getReadableDatabase();
             Cursor c = db.rawQuery(q, new String[]{tableNum})) {
            while (c.moveToNext()) list.add(OrderItem.fromCursor(c));
        }
        return list;
    }

    public List<TableSummary> getOpenTables() {
        List<TableSummary> list = new ArrayList<>();
        String q = "SELECT o.table_num,SUM(oi.quantity*oi.price_lbp) as total," +
                "MIN(o.created_at) as first_order FROM orders o " +
                "JOIN order_items oi ON oi.order_id=o.id " +
                "WHERE o.status='open' GROUP BY o.table_num ORDER BY o.table_num";
        try (SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery(q, null)) {
            while (c.moveToNext()) {
                TableSummary ts = new TableSummary();
                ts.tableNum = c.getString(0);
                ts.total = c.getLong(1);
                ts.firstOrder = c.getString(2);
                list.add(ts);
            }
        }
        return list;
    }

    public void closeTable(String tableNum, long totalLbp) {
        getWritableDatabase().execSQL(
                "UPDATE orders SET status='closed',total_lbp=? WHERE table_num=? AND status='open'",
                new Object[]{totalLbp, tableNum});
    }

    public void deleteOrderItem(int itemId) {
        getWritableDatabase().delete("order_items", "id=?",
                new String[]{String.valueOf(itemId)});
    }

    public void updateOrderItemQty(int itemId, int qty) {
        ContentValues cv = new ContentValues();
        cv.put("quantity", qty);
        getWritableDatabase().update("order_items", cv, "id=?",
                new String[]{String.valueOf(itemId)});
    }

    // Reports
    public List<ReportItem> getDailyReport() {
        return getReport("DATE(o.created_at)=DATE('now','localtime')");
    }

    public List<ReportItem> getMonthlyReport() {
        return getReport("strftime('%Y-%m',o.created_at)=strftime('%Y-%m','now','localtime')");
    }

    private List<ReportItem> getReport(String filter) {
        List<ReportItem> list = new ArrayList<>();
        String q = "SELECT o.table_num,o.server_name,oi.dish_name_ar,oi.dish_name_en," +
                "oi.quantity,oi.price_lbp,(oi.quantity*oi.price_lbp) as subtotal " +
                "FROM orders o JOIN order_items oi ON oi.order_id=o.id " +
                "WHERE o.status='closed' AND " + filter + " ORDER BY o.table_num,o.created_at";
        try (SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery(q, null)) {
            while (c.moveToNext()) {
                ReportItem ri = new ReportItem();
                ri.tableNum = c.getString(0); ri.server = c.getString(1);
                ri.dishNameAr = c.getString(2); ri.dishNameEn = c.getString(3);
                ri.quantity = c.getInt(4); ri.price = c.getInt(5);
                ri.subtotal = c.getLong(6);
                list.add(ri);
            }
        }
        return list;
    }

    public List<TopDish> getTopDishes() {
        List<TopDish> list = new ArrayList<>();
        String q = "SELECT oi.dish_name_ar,oi.dish_name_en,oi.cat_ar," +
                "SUM(oi.quantity) as qty,SUM(oi.quantity*oi.price_lbp) as revenue " +
                "FROM order_items oi JOIN orders o ON oi.order_id=o.id " +
                "WHERE o.status='closed' GROUP BY oi.dish_id ORDER BY qty DESC LIMIT 30";
        try (SQLiteDatabase db = getReadableDatabase(); Cursor c = db.rawQuery(q, null)) {
            while (c.moveToNext()) {
                TopDish td = new TopDish();
                td.nameAr = c.getString(0); td.nameEn = c.getString(1);
                td.catAr = c.getString(2); td.qty = c.getInt(3);
                td.revenue = c.getLong(4);
                list.add(td);
            }
        }
        return list;
    }

    public void clearOldRecords(int days) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM order_items WHERE order_id IN " +
                "(SELECT id FROM orders WHERE status='closed' AND " +
                "created_at < datetime('now','localtime','-" + days + " days'))");
        db.execSQL("DELETE FROM orders WHERE status='closed' AND " +
                "created_at < datetime('now','localtime','-" + days + " days')");
    }

    public void clearAllClosed() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM order_items WHERE order_id IN " +
                "(SELECT id FROM orders WHERE status='closed')");
        db.execSQL("DELETE FROM orders WHERE status='closed'");
    }
}
