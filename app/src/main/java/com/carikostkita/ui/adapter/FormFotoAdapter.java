package com.carikostkita.ui.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.carikostkita.R;
import com.carikostkita.data.model.FotoKost;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FormFotoAdapter extends RecyclerView.Adapter<FormFotoAdapter.ViewHolder> {

    public interface OnFotoActionListener {
        void onDeleteFoto(int position);
        void onSetAsCover(int position);
    }

    private final Context context;
    private final List<FotoKost> fotoList = new ArrayList<>();
    private final OnFotoActionListener listener;

    public FormFotoAdapter(Context context, OnFotoActionListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setFotos(List<FotoKost> list) {
        this.fotoList.clear();
        if (list != null) {
            this.fotoList.addAll(list);
        }
        notifyDataSetChanged();
    }

    public void addFoto(FotoKost foto) {
        if (fotoList.isEmpty()) {
            foto.setThumbnail(true);
        }
        this.fotoList.add(foto);
        notifyItemInserted(fotoList.size() - 1);
    }

    public void removeFoto(int position) {
        if (position >= 0 && position < fotoList.size()) {
            boolean wasCover = fotoList.get(position).isThumbnail();
            fotoList.remove(position);
            if (wasCover && !fotoList.isEmpty()) {
                fotoList.get(0).setThumbnail(true);
            }
            notifyDataSetChanged();
        }
    }

    public void setCover(int position) {
        if (position >= 0 && position < fotoList.size()) {
            for (int i = 0; i < fotoList.size(); i++) {
                fotoList.get(i).setThumbnail(i == position);
            }
            notifyDataSetChanged();
        }
    }

    public List<FotoKost> getFotoList() {
        return new ArrayList<>(fotoList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_form_foto, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        FotoKost item = fotoList.get(position);

        String path = item.getPathFile();
        if (path != null && !path.isEmpty()) {
            if (path.startsWith("http://") || path.startsWith("https://")) {
                Glide.with(context).load(path).placeholder(R.drawable.ic_bed).into(holder.ivThumb);
            } else if (path.startsWith("content://") || path.startsWith("file://")) {
                Glide.with(context).load(Uri.parse(path)).placeholder(R.drawable.ic_bed).into(holder.ivThumb);
            } else {
                File file = new File(path);
                if (file.exists()) {
                    Glide.with(context).load(file).placeholder(R.drawable.ic_bed).into(holder.ivThumb);
                } else {
                    Glide.with(context).load(R.drawable.ic_bed).into(holder.ivThumb);
                }
            }
        } else {
            holder.ivThumb.setImageResource(R.drawable.ic_bed);
        }

        if (item.isThumbnail()) {
            holder.tvBadgeCover.setVisibility(View.VISIBLE);
            holder.btnSetCover.setVisibility(View.GONE);
        } else {
            holder.tvBadgeCover.setVisibility(View.GONE);
            holder.btnSetCover.setVisibility(View.VISIBLE);
        }

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteFoto(holder.getAdapterPosition());
        });

        holder.btnSetCover.setOnClickListener(v -> {
            if (listener != null) listener.onSetAsCover(holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return fotoList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;
        TextView tvBadgeCover;
        ImageButton btnDelete;
        TextView btnSetCover;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_form_foto_thumb);
            tvBadgeCover = itemView.findViewById(R.id.tv_badge_cover);
            btnDelete = itemView.findViewById(R.id.btn_form_foto_delete);
            btnSetCover = itemView.findViewById(R.id.btn_set_cover);
        }
    }
}
