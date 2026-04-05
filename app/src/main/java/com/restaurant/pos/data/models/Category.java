package com.restaurant.pos.data.models;
import android.database.Cursor;

public class Category {
    public int id;
    public String nameAr, nameEn, printerIp;
    public static Category fromCursor(Cursor c) {
        Category o = new Category();
        o.id = c.getInt(c.getColumnIndexOrThrow("id"));
        o.nameAr = c.getString(c.getColumnIndexOrThrow("name_ar"));
        o.nameEn = c.getString(c.getColumnIndexOrThrow("name_en"));
        o.printerIp = c.getString(c.getColumnIndexOrThrow("printer_ip"));
        return o;
    }
}
