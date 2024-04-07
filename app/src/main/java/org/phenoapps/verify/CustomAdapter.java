package org.phenoapps.verify;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder>{


    ArrayList<ValueModel> values;
    boolean auxValues = false;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView textView;

        public ViewHolder(View view) {
            super(view);
            // Define click listener for the ViewHolder's View

            textView = (TextView) view.findViewById(R.id.textView_item);
        }

        public TextView getTextView() {
            return textView;
        }
    }

    public void setAuxValues(boolean auxValues) {
        this.auxValues = auxValues;
    }

    public CustomAdapter(ArrayList<ValueModel> fieldValues){
        this.values = fieldValues;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.value_item, parent, false);

        return new ViewHolder(view);
    }



    @Override
    public void onBindViewHolder(@NonNull CustomAdapter.ViewHolder holder, int position) {
        ValueModel value = values.get(position);
        if(value.getAuxValue() && !this.auxValues){
            return;
        }
        holder.getTextView().setText( value.getPrefix() +" : "+value.getValue());
    }

    @Override
    public int getItemCount() {
        return values.size();
    }
}
