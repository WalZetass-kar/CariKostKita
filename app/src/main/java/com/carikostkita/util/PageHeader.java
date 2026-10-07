package com.carikostkita.util;

import android.view.View;
import android.widget.TextView;
import com.carikostkita.R;

/** Mengisi header halaman bermotif dari layout view_page_header. */
public final class PageHeader {

    private PageHeader() {}

    public static void bind(View root, String title, String subtitle) {
        if (root == null) return;
        View header = root.findViewById(R.id.page_header_root);
        if (header == null) return;
        // Siluet & ornamen dipotong mengikuti sudut melengkung
        header.setClipToOutline(true);
        TextView tvTitle = header.findViewById(R.id.tv_page_header_title);
        TextView tvSub = header.findViewById(R.id.tv_page_header_subtitle);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSub != null) {
            tvSub.setText(subtitle);
            tvSub.setVisibility(subtitle == null || subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
}
