package org.phenoapps.verify;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

    ArrayList<ValueModel> values;
    boolean auxValues = false;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView labelView;
        private final TextView valueView;

        public ViewHolder(View view) {
            super(view);
            labelView = view.findViewById(R.id.textView_label);
            valueView = view.findViewById(R.id.textView_item);
        }

        public TextView getLabelView() { return labelView; }
        public TextView getValueView() { return valueView; }
    }

    public void setAuxValues(boolean auxValues) {
        this.auxValues = auxValues;
    }

    public CustomAdapter(ArrayList<ValueModel> fieldValues) {
        this.values = fieldValues;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.value_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ValueModel value = values.get(position);
        if (value.getAuxValue() && !this.auxValues) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
            return;
        }
        holder.itemView.setVisibility(View.VISIBLE);
        holder.itemView.setLayoutParams(
                new RecyclerView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
        holder.getLabelView().setText(value.getPrefix());
        holder.getValueView().setText(value.getValue());
    }

    @Override
    public int getItemCount() {
        return values.size();
    }
}
