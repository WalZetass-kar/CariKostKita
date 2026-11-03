package com.carikostkita.ui.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.RoommatePost;
import com.google.android.material.button.MaterialButton;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RoommateAdapter extends RecyclerView.Adapter<RoommateAdapter.ViewHolder> {

    private final Context context;
    private final List<RoommatePost> list = new ArrayList<>();

    public RoommateAdapter(Context context) {
        this.context = context;
    }

    public void setData(List<RoommatePost> posts) {
        list.clear();
        if (posts != null) {
            list.addAll(posts);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_roommate_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoommatePost item = list.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatarInitial;
        private final TextView tvName;
        private final TextView tvFaculty;
        private final TextView tvGender;
        private final TextView tvCampus;
        private final TextView tvRoomSpec;
        private final TextView tvArea;
        private final TextView tvDesc;
        private final TextView tvBudget;
        private final MaterialButton btnContactWa;
        private final MaterialButton btnShare;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarInitial = itemView.findViewById(R.id.tv_avatar_initial);
            tvName = itemView.findViewById(R.id.tv_roommate_name);
            tvFaculty = itemView.findViewById(R.id.tv_roommate_faculty);
            tvGender = itemView.findViewById(R.id.tv_roommate_gender);
            tvCampus = itemView.findViewById(R.id.tv_roommate_campus);
            tvRoomSpec = itemView.findViewById(R.id.tv_roommate_room_spec);
            tvArea = itemView.findViewById(R.id.tv_roommate_area);
            tvDesc = itemView.findViewById(R.id.tv_roommate_desc);
            tvBudget = itemView.findViewById(R.id.tv_roommate_budget);
            btnContactWa = itemView.findViewById(R.id.btn_contact_wa);
            btnShare = itemView.findViewById(R.id.btn_share_roommate);
        }

        void bind(RoommatePost item) {
            String name = item.getName();
            tvName.setText(name);

            // Avatar initial
            String initial = (!name.isEmpty()) ? name.substring(0, 1).toUpperCase() : "?";
            tvAvatarInitial.setText(initial);

            // Fakultas
            String faculty = item.getFaculty();
            if (faculty != null && !faculty.trim().isEmpty()) {
                tvFaculty.setVisibility(View.VISIBLE);
                tvFaculty.setText(faculty);
            } else {
                tvFaculty.setVisibility(View.GONE);
            }

            // Gender badge
            boolean isPutri = "PUTRI".equalsIgnoreCase(item.getGender());
            tvGender.setText(isPutri ? "Putri" : "Putra");
            tvGender.setBackgroundResource(isPutri ? R.drawable.bg_badge_putri : R.drawable.bg_badge_putra);
            tvGender.setTextColor(ContextCompat.getColor(context, isPutri ? R.color.badge_putri : R.color.badge_putra));

            // Campus
            tvCampus.setText(item.getCampus());

            // Room spec
            String spec = item.getRoomType();
            if (spec != null && !spec.trim().isEmpty()) {
                tvRoomSpec.setVisibility(View.VISIBLE);
                tvRoomSpec.setText(spec);
            } else {
                tvRoomSpec.setVisibility(View.GONE);
            }

            // Target Area
            tvArea.setText(item.getTargetArea());

            // Deskripsi
            tvDesc.setText(item.getDescription());

            // Budget
            tvBudget.setText(item.getFormattedBudget());

            // WhatsApp Action
            btnContactWa.setOnClickListener(v -> {
                String rawWa = item.getContactWhatsapp();
                if (rawWa == null || rawWa.trim().isEmpty()) {
                    Toast.makeText(context, "Nomor WhatsApp belum tersedia.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String phone = rawWa.replaceAll("[^0-9]", "");
                if (phone.startsWith("0")) {
                    phone = "62" + phone.substring(1);
                }

                String greeting = "Halo kak " + item.getName() + ", saya melihat postingan kamu di CariKostKita yang sedang mencari teman sekamar di "
                        + item.getCampus() + " (budget patungan " + item.getFormattedBudget() + "). Apakah masih mencari teman sekamar? Boleh ngobrol lebih lanjut?";

                try {
                    String url = "https://api.whatsapp.com/send?phone=" + phone + "&text=" + URLEncoder.encode(greeting, StandardCharsets.UTF_8.name());
                    Intent waIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(waIntent);
                } catch (Exception e) {
                    Toast.makeText(context, "Gagal membuka WhatsApp: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            // Share Action
            btnShare.setOnClickListener(v -> {
                String shareText = "Cari Teman Sekamar Kampus!\n\n"
                        + item.getName() + " (" + (isPutri ? "Mahasiswi Putri" : "Mahasiswa Putra") + ") sedang mencari teman sekamar di sekitar "
                        + item.getCampus() + ".\n"
                        + "Budget patungan: " + item.getFormattedBudget() + "\n"
                        + "Lokasi diincar: " + item.getTargetArea() + "\n"
                        + "Kriteria: \"" + item.getDescription() + "\"\n\n"
                        + "Temukan dan hubungi langsung melalui aplikasi CariKostKita!";

                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, shareText);
                sendIntent.setType("text/plain");
                context.startActivity(Intent.createChooser(sendIntent, "Bagikan Info Teman Sekamar"));
            });
        }
    }
}
