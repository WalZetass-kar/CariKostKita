package com.carikostkita.ui.survey;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.carikostkita.R;
import com.carikostkita.data.model.SurveyRequest;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.SurveyRepository;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.util.AppDialogHelper;
import com.carikostkita.util.PageHeader;
import com.carikostkita.util.SessionManager;
import com.google.android.material.button.MaterialButton;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** Jadwal survei: pencari melihat & membatalkan, pemilik mengonfirmasi / menolak / menandai selesai. */
public class SurveyListActivity extends AppCompatActivity {

    private SurveyRepository repo;
    private boolean asOwner;
    private final List<SurveyRequest> items = new ArrayList<>();
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_survey_list);
        SessionManager session = new SessionManager(this);
        if (!session.isLoggedIn()) {
            finish();
            return;
        }
        asOwner = session.isPemilikKost();
        repo = new SurveyRepository(this);
        PageHeader.bind(findViewById(android.R.id.content), "Jadwal Survei",
                asOwner ? "Konfirmasi kunjungan calon penyewa" : "Kunjungan kost yang kamu ajukan");
        findViewById(R.id.btn_survey_back).setOnClickListener(v -> finish());
        RecyclerView rv = findViewById(R.id.rv_surveys);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new Adapter();
        rv.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        findViewById(R.id.pb_surveys).setVisibility(View.VISIBLE);
        repo.list(!asOwner, new DataCallback<List<SurveyRequest>>() {
            @Override
            public void onSuccess(List<SurveyRequest> data) {
                findViewById(R.id.pb_surveys).setVisibility(View.GONE);
                items.clear();
                items.addAll(data);
                adapter.notifyDataSetChanged();
                TextView empty = findViewById(R.id.tv_surveys_empty);
                empty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                empty.setText(asOwner ? "Belum ada permintaan survei. Permintaan baru akan muncul di sini dan sebagai notifikasi."
                        : "Belum ada jadwal survei. Buka halaman kost lalu ketuk \"Jadwalkan Survei\".");
            }

            @Override
            public void onError(String message) {
                findViewById(R.id.pb_surveys).setVisibility(View.GONE);
                TextView empty = findViewById(R.id.tv_surveys_empty);
                empty.setVisibility(View.VISIBLE);
                empty.setText(message);
            }
        });
    }

    private void change(SurveyRequest s, String status, String alasan) {
        repo.updateStatus(s.id, status, alasan, new DataCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                load();
            }

            @Override
            public void onError(String message) {
                Toast.makeText(SurveyListActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    static String when(String iso) {
        if (iso == null || iso.length() < 19) return "";
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            in.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
            Date d = in.parse(iso.substring(0, 19));
            return d == null ? "" : new SimpleDateFormat("EEEE, d MMM yyyy • HH:mm", new Locale("in", "ID")).format(d);
        } catch (Exception e) {
            return iso;
        }
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.VH> {
        class VH extends RecyclerView.ViewHolder {
            final TextView kost, status, whenTv, meta;
            final LinearLayout actions;

            VH(View v) {
                super(v);
                kost = v.findViewById(R.id.tv_survey_kost);
                status = v.findViewById(R.id.tv_survey_status);
                whenTv = v.findViewById(R.id.tv_survey_when);
                meta = v.findViewById(R.id.tv_survey_meta);
                actions = v.findViewById(R.id.layout_survey_actions);
            }
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_survey, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            SurveyRequest s = items.get(position);
            h.kost.setText(s.namaKost != null ? s.namaKost : "Kost");
            h.status.setText(s.getStatusLabel());
            boolean good = SurveyRequest.DIKONFIRMASI.equals(s.status) || SurveyRequest.SELESAI.equals(s.status);
            boolean bad = SurveyRequest.DITOLAK.equals(s.status) || SurveyRequest.DIBATALKAN.equals(s.status);
            h.status.setBackgroundResource(bad ? R.drawable.bg_pill_penuh : R.drawable.bg_pill_tersedia);
            h.status.setTextColor(ContextCompat.getColor(SurveyListActivity.this,
                    bad ? R.color.status_penuh : good ? R.color.status_tersedia : R.color.primary));
            h.whenTv.setText(when(s.jadwal));
            StringBuilder meta = new StringBuilder();
            if (asOwner) meta.append("Pengaju: ").append(s.namaPencari != null ? s.namaPencari : "Calon penyewa");
            if (s.catatan != null && !s.catatan.isEmpty()) meta.append(meta.length() > 0 ? "\n" : "").append("Catatan: ").append(s.catatan);
            if (s.alasan != null && !s.alasan.isEmpty()) meta.append(meta.length() > 0 ? "\n" : "").append("Alasan: ").append(s.alasan);
            h.meta.setText(meta.toString());
            h.meta.setVisibility(meta.length() == 0 ? View.GONE : View.VISIBLE);

            h.actions.removeAllViews();
            if (asOwner && SurveyRequest.MENUNGGU.equals(s.status)) {
                addAction(h.actions, "Tolak", false, v -> askReason(s, SurveyRequest.DITOLAK));
                addAction(h.actions, "Konfirmasi", true, v -> change(s, SurveyRequest.DIKONFIRMASI, null));
            } else if (asOwner && SurveyRequest.DIKONFIRMASI.equals(s.status)) {
                addAction(h.actions, "Batalkan", false, v -> askReason(s, SurveyRequest.DITOLAK));
                addAction(h.actions, "Tandai selesai", true, v -> change(s, SurveyRequest.SELESAI, null));
            } else if (!asOwner && s.isActive()) {
                addAction(h.actions, "Batalkan", false, v -> AppDialogHelper.showConfirm(SurveyListActivity.this,
                        "Batalkan survei?", "Pemilik akan melihat bahwa kamu membatalkan jadwal ini.", "Batalkan",
                        () -> change(s, SurveyRequest.DIBATALKAN, null)));
                addAction(h.actions, "Lihat kost", true, v -> openKost(s));
            } else if (!asOwner && SurveyRequest.SELESAI.equals(s.status)) {
                addAction(h.actions, "Tulis ulasan", true, v -> openKost(s));
            }
            h.actions.setVisibility(h.actions.getChildCount() == 0 ? View.GONE : View.VISIBLE);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private void askReason(SurveyRequest s, String status) {
        AppDialogHelper.showInput(this, "Alasan untuk calon penyewa",
                "Beri tahu alasannya atau tawarkan waktu lain.", "Contoh: hari itu saya di luar kota, bisa Sabtu?",
                "", "Kirim", text -> change(s, status, text.isEmpty() ? null : text));
    }

    private void openKost(SurveyRequest s) {
        Intent i = new Intent(this, DetailKostActivity.class);
        i.putExtra("kost_id", s.kostId);
        startActivity(i);
    }

    private void addAction(LinearLayout parent, String label, boolean primary, View.OnClickListener click) {
        MaterialButton b = new MaterialButton(this, null, primary
                ? com.google.android.material.R.attr.materialButtonStyle
                : com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(label);
        b.setAllCaps(false);
        if (primary) {
            b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary)));
            b.setTextColor(ContextCompat.getColor(this, R.color.on_primary));
        } else {
            b.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        if (parent.getChildCount() > 0) lp.setMarginStart((int) (8 * getResources().getDisplayMetrics().density));
        b.setOnClickListener(click);
        parent.addView(b, lp);
    }
}
