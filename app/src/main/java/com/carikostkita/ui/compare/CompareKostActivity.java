package com.carikostkita.ui.compare;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.carikostkita.R;
import com.carikostkita.data.model.Fasilitas;
import com.carikostkita.data.model.Kost;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.data.repository.KostRepository;
import com.carikostkita.ui.detail.DetailKostActivity;
import com.carikostkita.util.FormatUtil;
import com.carikostkita.util.GeoUtil;
import com.carikostkita.util.PageHeader;
import com.carikostkita.util.SessionManager;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Tabel perbandingan 2–3 kost: baris = atribut, kolom = kost. Nilai terbaik diberi warna. */
public class CompareKostActivity extends AppCompatActivity {
    public static final String EXTRA_IDS = "kost_ids";

    private final List<Kost> kosts = new ArrayList<>();
    private int pending;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compare_kost);
        PageHeader.bind(findViewById(android.R.id.content), "Bandingkan Kost", "Geser ke samping untuk melihat semua kolom");
        findViewById(R.id.btn_compare_back).setOnClickListener(v -> finish());
        ArrayList<String> ids = getIntent().getStringArrayListExtra(EXTRA_IDS);
        if (ids == null || ids.size() < 2) {
            finish();
            return;
        }
        KostRepository repo = new KostRepository(this);
        String uid = new SessionManager(this).getUserUid();
        pending = ids.size();
        Kost[] slots = new Kost[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            final int idx = i;
            repo.getKostDetail(ids.get(i), uid, new DataCallback<Kost>() {
                @Override
                public void onSuccess(Kost data) {
                    slots[idx] = data;
                    done(slots);
                }

                @Override
                public void onError(String message) {
                    done(slots);
                }
            });
        }
    }

    private void done(Kost[] slots) {
        if (--pending > 0 || isFinishing()) return;
        findViewById(R.id.pb_compare).setVisibility(View.GONE);
        for (Kost k : slots) if (k != null) kosts.add(k);
        render();
    }

    private void render() {
        TableLayout table = findViewById(R.id.table_compare);
        table.removeAllViews();
        SessionManager s = new SessionManager(this);
        double lat = s.getUserSelectedLat(), lng = s.getUserSelectedLng();

        TableRow photos = new TableRow(this);
        photos.addView(label(""));
        for (Kost k : kosts) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setContentDescription("Foto " + k.getNamaKost());
            TableRow.LayoutParams lp = new TableRow.LayoutParams(dp(150), dp(100));
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            iv.setLayoutParams(lp);
            iv.setBackgroundResource(R.drawable.bg_thumb_placeholder);
            Glide.with(this).load(k.getThumbnailUrl()).centerCrop().into(iv);
            iv.setOnClickListener(v -> startActivity(new Intent(this, DetailKostActivity.class).putExtra("kost_id", k.getId())));
            photos.addView(iv);
        }
        table.addView(photos);

        double minPrice = Double.MAX_VALUE;
        for (Kost k : kosts) minPrice = Math.min(minPrice, k.getHarga());
        List<String> names = new ArrayList<>(), prices = new ArrayList<>(), types = new ArrayList<>(), rooms = new ArrayList<>(),
                sizes = new ArrayList<>(), dist = new ArrayList<>(), deposits = new ArrayList<>(), mins = new ArrayList<>(),
                ratings = new ArrayList<>(), rules = new ArrayList<>(), fees = new ArrayList<>();
        List<Boolean> bestPrice = new ArrayList<>();
        for (Kost k : kosts) {
            names.add(k.getNamaKost());
            prices.add(FormatUtil.formatRupiah(k.getHarga()));
            bestPrice.add(k.getHarga() == minPrice);
            types.add(k.getTipeKost() != null ? k.getTipeKost().name().charAt(0) + k.getTipeKost().name().substring(1).toLowerCase() : "–");
            rooms.add(k.getAvailabilityLabel());
            sizes.add(k.getUkuranKamar() != null ? k.getUkuranKamar() : "–");
            dist.add(lat != 0 && k.hasCoordinates() ? GeoUtil.formatDistance(GeoUtil.distanceKm(lat, lng, k.getLatitude(), k.getLongitude())) : "–");
            deposits.add(k.getDeposit() != null ? FormatUtil.formatRupiah(k.getDeposit()) : "–");
            mins.add(k.getMinimalSewaBulan() != null ? k.getMinimalSewaBulan() + " bulan" : "–");
            fees.add(k.getBiayaTambahan() != null ? k.getBiayaTambahan() : "–");
            ratings.add(k.getRatingLabel() != null ? k.getRatingLabel() : "Belum ada");
            rules.add(k.getAturan().isEmpty() ? "–" : android.text.TextUtils.join("\n", k.getAturan()));
        }
        addRow(table, "Nama", names, null, true);
        addRow(table, "Harga/bulan", prices, bestPrice, true);
        addRow(table, "Tipe", types, null, false);
        addRow(table, "Ketersediaan", rooms, null, false);
        addRow(table, "Ukuran kamar", sizes, null, false);
        addRow(table, "Jarak", dist, null, false);
        addRow(table, "Rating", ratings, null, false);
        addRow(table, "Deposit", deposits, null, false);
        addRow(table, "Minimal sewa", mins, null, false);
        addRow(table, "Biaya lain", fees, null, false);

        Set<String> allFas = new LinkedHashSet<>();
        for (Fasilitas f : Fasilitas.getMaster()) {
            for (Kost k : kosts) if (k.getFasilitas().contains(f.getNamaFasilitas())) allFas.add(f.getNamaFasilitas());
        }
        for (String fas : allFas) {
            List<String> vals = new ArrayList<>();
            List<Boolean> has = new ArrayList<>();
            for (Kost k : kosts) {
                boolean h = k.getFasilitas().contains(fas);
                vals.add(h ? "Ada" : "–");
                has.add(h);
            }
            addRow(table, fas, vals, has, false);
        }
        addRow(table, "Aturan", rules, null, false);
    }

    private void addRow(TableLayout table, String title, List<String> values, List<Boolean> highlight, boolean bold) {
        TableRow row = new TableRow(this);
        row.setBackgroundResource(table.getChildCount() % 2 == 0 ? android.R.color.transparent : R.color.primary_soft);
        row.addView(label(title));
        for (int i = 0; i < values.size(); i++) {
            TextView tv = new TextView(this);
            tv.setText(values.get(i));
            tv.setTextSize(13);
            tv.setPadding(dp(8), dp(10), dp(8), dp(10));
            tv.setMaxWidth(dp(150));
            boolean good = highlight != null && Boolean.TRUE.equals(highlight.get(i));
            tv.setTextColor(ContextCompat.getColor(this, good ? R.color.status_tersedia : R.color.text_primary));
            if (bold || good) tv.setTypeface(null, Typeface.BOLD);
            row.addView(tv);
        }
        table.addView(row);
    }

    private TextView label(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTypeface(null, Typeface.BOLD);
        tv.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        tv.setPadding(dp(4), dp(10), dp(8), dp(10));
        tv.setMaxWidth(dp(110));
        return tv;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
