package com.carikostkita.ui.main;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.carikostkita.R;
import com.carikostkita.ui.main.chat.ChatListFragment;
import com.carikostkita.ui.main.favorite.FavoriteFragment;
import com.carikostkita.ui.main.home.HomeFragment;
import com.carikostkita.ui.main.profile.ProfileFragment;
import com.carikostkita.ui.main.search.SearchFragment;

public class MainActivity extends AppCompatActivity {

    private ModernBottomNavBar bottomNav;
    private int currentTabIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.modern_bottom_nav);
        bottomNav.setOnTabSelectedListener(this::switchTab);

        // Set default fragment
        if (savedInstanceState == null) {
            switchTab(0);
        }
    }

    public void navigateToSearch() {
        if (bottomNav != null) {
            bottomNav.selectTab(1, true);
            switchTab(1);
        }
    }

    public void navigateToChat() {
        if (bottomNav != null) {
            bottomNav.selectTab(3, true);
            switchTab(3);
        }
    }

    private void switchTab(int index) {
        if (currentTabIndex == index) return;
        currentTabIndex = index;

        Fragment selectedFragment;
        switch (index) {
            case 1:
                selectedFragment = new SearchFragment();
                break;
            case 2:
                selectedFragment = new FavoriteFragment();
                break;
            case 3:
                selectedFragment = new ChatListFragment();
                break;
            case 4:
                selectedFragment = new ProfileFragment();
                break;
            case 0:
            default:
                selectedFragment = new HomeFragment();
                break;
        }

        getSupportFragmentManager().beginTransaction()
                .setCustomAnimations(R.anim.tab_enter, R.anim.tab_exit)
                .replace(R.id.fragment_container, selectedFragment)
                .commit();
    }
}
