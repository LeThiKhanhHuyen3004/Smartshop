package com.example.app_technical_admin.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.app_technical_admin.R;
import com.example.app_technical_admin.model.TypeOfProduct;

import java.util.List;

public class TypeOfProductAdapter extends BaseAdapter {
    List<TypeOfProduct> array;
    Context context;

    public TypeOfProductAdapter(Context context, List<TypeOfProduct> array) {
        this.context = context;
        this.array = array;
    }

    @Override
    public int getCount() {
        return array.size();
    }

    @Override
    public Object getItem(int i) {
        return null;
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    public class ViewHolder{
        TextView textProductName;
        ImageView imgImage;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder = null;
        if(view == null){
            viewHolder = new ViewHolder();
            LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.item_product, null);
            viewHolder.textProductName = view.findViewById(R.id.item_productName);
            viewHolder.imgImage = view.findViewById(R.id.item_image);
            view.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) view.getTag();

        }
        viewHolder.textProductName.setText(array.get(i).getProductName());
        Log.d("GlideImage", "Loading image URL: " + array.get(i).getImage());
        Glide.with(context).load(array.get(i).getImage()).into(viewHolder.imgImage);
        return view;
    }
}
