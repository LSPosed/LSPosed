package org.meowcat.edxposed.manager.util.json;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class XposedZip {

    public String name;
    public String link;
    public String version;
    public String description;

    public static class MyAdapter extends ArrayAdapter<XposedZip> {

        private final Context context;
        List<XposedZip> list;

        public MyAdapter(Context context, List<XposedZip> objects) {
            super(context, android.R.layout.simple_dropdown_item_1line, objects);
            this.context = context;
            this.list = objects;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getMyView(parent, position);
        }

        @Override
        public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            return getMyView(parent, position);
        }

        private View getMyView(ViewGroup parent, int position) {
            View row;
            ItemHolder holder = new ItemHolder();

            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);

            holder.name = row.findViewById(android.R.id.text1);

            row.setTag(holder);

            holder.name.setText(list.get(position).name);
            return row;
        }

        private class ItemHolder {
            TextView name;
        }

    }

}