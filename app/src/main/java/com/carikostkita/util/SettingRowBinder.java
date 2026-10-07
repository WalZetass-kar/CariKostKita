package com.carikostkita.util;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.carikostkita.R;
import com.google.android.material.materialswitch.MaterialSwitch;

/** Mengisi baris menu seragam dari layout item_setting_row / item_value_row. */
public final class SettingRowBinder {

    private SettingRowBinder() {}

    public static View bind(View row, int iconRes, String title, String subtitle, View.OnClickListener onClick) {
        if (row == null) return null;
        ImageView icon = row.findViewById(R.id.iv_setting_icon);
        TextView tvTitle = row.findViewById(R.id.tv_setting_title);
        TextView tvSub = row.findViewById(R.id.tv_setting_subtitle);
        if (icon != null) icon.setImageResource(iconRes);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvSub != null) {
            tvSub.setText(subtitle);
            tvSub.setVisibility(subtitle == null || subtitle.isEmpty() ? View.GONE : View.VISIBLE);
        }
        row.setContentDescription(subtitle == null ? title : title + ". " + subtitle);
        if (onClick != null) row.setOnClickListener(onClick);
        return row;
    }

    /** Baris dengan toggle di kanan; ketuk baris juga membalik toggle. */
    public static MaterialSwitch bindToggle(View row, int iconRes, String title, String subtitle,
                                            boolean checked, android.widget.CompoundButton.OnCheckedChangeListener listener) {
        bind(row, iconRes, title, subtitle, null);
        if (row == null) return null;
        MaterialSwitch sw = row.findViewById(R.id.sw_setting_toggle);
        View chevron = row.findViewById(R.id.iv_setting_chevron);
        if (chevron != null) chevron.setVisibility(View.GONE);
        if (sw == null) return null;
        sw.setVisibility(View.VISIBLE);
        sw.setChecked(checked);
        sw.setOnCheckedChangeListener(listener);
        row.setOnClickListener(v -> sw.toggle());
        return sw;
    }

    public static void bindValue(View row, int iconRes, String title, String desc) {
        if (row == null) return;
        ImageView icon = row.findViewById(R.id.iv_value_icon);
        TextView tvTitle = row.findViewById(R.id.tv_value_title);
        TextView tvDesc = row.findViewById(R.id.tv_value_desc);
        if (icon != null) icon.setImageResource(iconRes);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvDesc != null) tvDesc.setText(desc);
    }
}
