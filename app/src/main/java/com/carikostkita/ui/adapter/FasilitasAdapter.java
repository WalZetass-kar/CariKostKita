package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Fasilitas;
import java.util.ArrayList;
import java.util.List;

public class FasilitasAdapter extends RecyclerView.Adapter<FasilitasAdapter.FasilitasViewHolder> {

    private final List<Fasilitas> list;

    public FasilitasAdapter() {
        this.list = new ArrayList<>();
    }

    public void submitList(List<Fasilitas> newList) {
        this.list.clear();
        if (newList != null) {
            this.list.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FasilitasViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_fasilitas_chip, parent, false);
        return new FasilitasViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FasilitasViewHolder holder, int position) {
        Fasilitas f = list.get(position);
        holder.tvName.setText(f.getNamaFasilitas());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class FasilitasViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvName;

        public FasilitasViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_fasilitas_name);
        }
    }
}
