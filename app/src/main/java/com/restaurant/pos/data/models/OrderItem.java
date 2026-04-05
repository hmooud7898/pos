package com.restaurant.pos.data.models;
import android.database.Cursor;

public class OrderItem {
    public int id, orderId, dishId, quantity, priceLbp;
    public String dishNameAr, dishNameEn, catEn, catAr, printerIp, note;

    public static OrderItem fromCursor(Cursor c) {
        OrderItem it = new OrderItem();
        it.id = c.getInt(c.getColumnIndexOrThrow("id"));
        it.orderId = c.getInt(c.getColumnIndexOrThrow("order_id"));
        it.dishId = c.getInt(c.getColumnIndexOrThrow("dish_id"));
        it.quantity = c.getInt(c.getColumnIndexOrThrow("quantity"));
        it.priceLbp = c.getInt(c.getColumnIndexOrThrow("price_lbp"));
        it.dishNameAr = c.getString(c.getColumnIndexOrThrow("dish_name_ar"));
        it.dishNameEn = c.getString(c.getColumnIndexOrThrow("dish_name_en"));
        it.catEn = c.getString(c.getColumnIndexOrThrow("cat_en"));
        it.catAr = c.getString(c.getColumnIndexOrThrow("cat_ar"));
        it.printerIp = c.getString(c.getColumnIndexOrThrow("printer_ip"));
        it.note = c.getString(c.getColumnIndexOrThrow("note"));
        return it;
    }

    public long subtotal() { return (long) quantity * priceLbp; }
}
