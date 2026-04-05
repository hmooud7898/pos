package com.restaurant.pos.data.models;
import android.database.Cursor;
import java.util.Locale;

public class Dish {
    public int id, categoryId, priceLbp;
    public String nameAr, nameEn, catAr, catEn, catPrinter;
    public boolean active;

    public static Dish fromCursor(Cursor c) {
        Dish d = new Dish();
        d.id = c.getInt(c.getColumnIndexOrThrow("id"));
        d.nameAr = c.getString(c.getColumnIndexOrThrow("name_ar"));
        d.nameEn = c.getString(c.getColumnIndexOrThrow("name_en"));
        d.categoryId = c.getInt(c.getColumnIndexOrThrow("category_id"));
        d.priceLbp = c.getInt(c.getColumnIndexOrThrow("price_lbp"));
        d.active = c.getInt(c.getColumnIndexOrThrow("active")) == 1;
        try { d.catAr = c.getString(c.getColumnIndexOrThrow("cat_ar")); } catch(Exception e){d.catAr="";}
        try { d.catEn = c.getString(c.getColumnIndexOrThrow("cat_en")); } catch(Exception e){d.catEn="";}
        try { d.catPrinter = c.getString(c.getColumnIndexOrThrow("cat_printer")); } catch(Exception e){d.catPrinter="";}
        return d;
    }

    public String priceFormatted() {
        if (priceLbp >= 1000000) return String.format(Locale.getDefault(),"%.1fM", priceLbp/1000000.0);
        if (priceLbp >= 1000) return (priceLbp/1000) + "k";
        return String.valueOf(priceLbp);
    }
}
