package com.carikostkita.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.carikostkita.R;
import com.carikostkita.data.model.FotoKost;
import java.util.ArrayList;
import java.util.List;

public class FotoSliderAdapter extends RecyclerView.Adapter<FotoSliderAdapter.SliderViewHolder> {

    private final List<FotoKost> fotoList;

    public FotoSliderAdapter() {
        this.fotoList = new ArrayList<>();
    }

    public void submitList(List<FotoKost> newList) {
        this.fotoList.clear();
        if (newList != null) {
            this.fotoList.addAll(newList);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_foto_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        if (!fotoList.isEmpty() && position < fotoList.size()) {
            FotoKost foto = fotoList.get(position);
            String path = foto != null ? foto.getPathFile() : null;
            if (path != null && !path.trim().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(path.trim())
                        .centerCrop()
                        .placeholder(R.drawable.bg_thumb_placeholder)
                        .error(R.drawable.bg_thumb_placeholder)
                        .into(holder.ivImage);
            } else {
                holder.ivImage.setImageResource(R.drawable.bg_thumb_placeholder);
            }
        } else {
            holder.ivImage.setImageResource(R.drawable.bg_thumb_placeholder);
        }
    }

    @Override
    public int getItemCount() {
        return fotoList.isEmpty() ? 1 : fotoList.size();
    }

    public static class SliderViewHolder extends RecyclerView.ViewHolder {
        public final ImageView ivImage;

        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_slider_image);
        }
    }
}
