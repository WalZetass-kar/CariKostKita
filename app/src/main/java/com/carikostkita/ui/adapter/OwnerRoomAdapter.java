package com.carikostkita.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.Room;
import com.carikostkita.util.FormatUtil;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

public class OwnerRoomAdapter extends RecyclerView.Adapter<OwnerRoomAdapter.ViewHolder> {

    public interface OnRoomActionListener {
        void onStatusChangeClick(Room room, int position);
        void onDeleteClick(Room room, int position);
    }

    private final List<Room> roomList = new ArrayList<>();
    private final OnRoomActionListener listener;

    public OwnerRoomAdapter(OnRoomActionListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Room> list) {
        this.roomList.clear();
        if (list != null) {
            this.roomList.addAll(list);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_owner_room, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Room room = roomList.get(position);
        Context ctx = holder.itemView.getContext();

        holder.tvName.setText(room.getName());
        holder.tvType.setText("Tipe: " + room.getRoomType());
        holder.tvPrice.setText(FormatUtil.formatRupiah((long) room.getPrice()) + " / bln");

        String status = room.getStatus().toUpperCase();
        holder.tvStatus.setText(status);

        if ("TERSEDIA".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_tersedia);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_tersedia));
        } else if ("TERISI".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_penuh);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.status_penuh));
        } else if ("BOOKING".equals(status)) {
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_putra);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.primary));
        } else {
            // MAINTENANCE
            holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_campur);
            holder.tvStatus.setTextColor(ContextCompat.getColor(ctx, R.color.badge_campur));
        }

        holder.tvStatus.setOnClickListener(v -> {
            if (listener != null) listener.onStatusChangeClick(room, position);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteClick(room, position);
        });
    }

    @Override
    public int getItemCount() {
        return roomList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName;
        TextView tvStatus;
        TextView tvType;
        TextView tvPrice;
        MaterialButton btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_room_name);
            tvStatus = itemView.findViewById(R.id.tv_room_status);
            tvType = itemView.findViewById(R.id.tv_room_type);
            tvPrice = itemView.findViewById(R.id.tv_room_price);
            btnDelete = itemView.findViewById(R.id.btn_room_delete);
        }
    }
}
