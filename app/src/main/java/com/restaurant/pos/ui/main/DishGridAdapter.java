package com.restaurant.pos.ui.main;
import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import com.restaurant.pos.data.models.Dish;
import java.util.List;

public class DishGridAdapter extends BaseAdapter {
    public interface OnDishClick { void onClick(Dish dish); }
    private final Context ctx;
    private final List<Dish> dishes;
    private final OnDishClick listener;

    public DishGridAdapter(Context ctx, List<Dish> dishes, OnDishClick listener) {
        this.ctx=ctx; this.dishes=dishes; this.listener=listener;
    }
    @Override public int getCount() { return dishes.size(); }
    @Override public Dish getItem(int i) { return dishes.get(i); }
    @Override public long getItemId(int i) { return i; }
    @Override
    public View getView(int pos, View cv, ViewGroup parent) {
        Button b = new Button(ctx);
        Dish d = dishes.get(pos);
        b.setText(d.nameAr + "\n" + d.priceFormatted());
        b.setBackgroundColor(Color.parseColor("#1a4a8a"));
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setOnClickListener(v -> listener.onClick(d));
        return b;
    }
}
