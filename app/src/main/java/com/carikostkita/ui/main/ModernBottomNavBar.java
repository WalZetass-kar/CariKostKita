package com.carikostkita.ui.main;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * ModernBottomNavBar mewarisi implementasi Floating Expanding Pill
 * dari AppBottomNavView untuk kompatibilitas retroaktif penuh di seluruh project.
 */
public class ModernBottomNavBar extends AppBottomNavView {

    public ModernBottomNavBar(@NonNull Context context) {
        super(context);
    }

    public ModernBottomNavBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ModernBottomNavBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
}
