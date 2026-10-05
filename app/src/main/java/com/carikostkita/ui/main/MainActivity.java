package com.carikostkita.ui.main;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.carikostkita.R;
import com.carikostkita.ui.admin.AdminMainActivity;
import com.carikostkita.ui.admin.PemilikMainActivity;
import com.carikostkita.ui.main.chat.ChatListFragment;
import com.carikostkita.ui.main.favorite.FavoriteFragment;
import com.carikostkita.ui.main.home.HomeFragment;
import com.carikostkita.ui.main.profile.ProfileFragment;
import com.carikostkita.ui.main.search.SearchFragment;
import com.carikostkita.util.SessionManager;
import java.lang.reflect.Field;

public class MainActivity extends AppCompatActivity {

    private AppBottomNavView bottomNav;
    private ViewPager2 viewPager;
    private long backPressedTime = 0;
    private Toast backToast;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Role Guard: MainActivity hanya untuk Pencari Kost (User) & Tamu
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            if (sessionManager.isDeveloper()) {
                startActivity(new Intent(this, AdminMainActivity.class));
                finish();
                return;
            } else if (sessionManager.isPemilikKost()) {
                startActivity(new Intent(this, PemilikMainActivity.class));
                finish();
                return;
            }
        }

        setContentView(R.layout.activity_main);

        initViews();
        setupViewPager();
        setupDoubleBackToExit();
    }

    private void initViews() {
        bottomNav = findViewById(R.id.modern_bottom_nav);
        viewPager = findViewById(R.id.main_view_pager);

        bottomNav.setOnTabSelectedListener(index -> {
            if (viewPager != null && viewPager.getCurrentItem() != index) {
                viewPager.setCurrentItem(index, true);
            }
        });
    }

    private void setupViewPager() {
        MainPagerAdapter adapter = new MainPagerAdapter(this);
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(4); // Cache all 5 tabs in memory for instantaneous switching
        viewPager.setPageTransformer(new com.carikostkita.ui.view.SmoothPageTransformer());

        // Atur touch slop 2x agar scroll vertikal di halaman tidak sengaja memicu swipe horizontal
        tuneTouchSlop(viewPager);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (bottomNav != null && bottomNav.getSelectedIndex() != position) {
                    bottomNav.selectTab(position, true);
                }
                if (position == 3 && bottomNav != null) {
                    bottomNav.setChatUnreadBadge(false);
                }
            }
        });
    }

    private void tuneTouchSlop(ViewPager2 pager) {
        try {
            Field recyclerField = ViewPager2.class.getDeclaredField("mRecyclerView");
            recyclerField.setAccessible(true);
            RecyclerView internalRecycler = (RecyclerView) recyclerField.get(pager);
            if (internalRecycler != null) {
                Field touchSlopField = RecyclerView.class.getDeclaredField("mTouchSlop");
                touchSlopField.setAccessible(true);
                int touchSlop = (int) touchSlopField.get(internalRecycler);
                touchSlopField.set(internalRecycler, touchSlop * 2);
            }
        } catch (Exception ignored) {}
    }

    private void setupDoubleBackToExit() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - backPressedTime < 2000) {
                    if (backToast != null) {
                        backToast.cancel();
                    }
                    finish();
                } else {
                    backPressedTime = currentTime;
                    if (backToast != null) {
                        backToast.cancel();
                    }
                    backToast = Toast.makeText(MainActivity.this,
                            "Tekan kembali sekali lagi untuk keluar", Toast.LENGTH_SHORT);
                    backToast.show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            if (sessionManager.isDeveloper()) {
                startActivity(new Intent(this, AdminMainActivity.class));
                finish();
                return;
            } else if (sessionManager.isPemilikKost()) {
                startActivity(new Intent(this, PemilikMainActivity.class));
                finish();
                return;
            }
            checkForUnreadNotifications();
        }
    }

    private void checkForUnreadNotifications() {
        SessionManager sessionManager = new SessionManager(this);
        if (!sessionManager.isLoggedIn()) return;
        String userUid = sessionManager.getUserUid();
        if (userUid == null || userUid.isEmpty()) return;

        com.carikostkita.data.repository.ChatRepository chatRepo = new com.carikostkita.data.repository.ChatRepository(this);
        chatRepo.getConversationsForUser(userUid, new com.carikostkita.data.repository.DataCallback<java.util.List<com.carikostkita.data.model.ChatConversation>>() {
            @Override
            public void onSuccess(java.util.List<com.carikostkita.data.model.ChatConversation> conversations) {
                if (conversations != null && !conversations.isEmpty()) {
                    int totalUnread = 0;
                    for (com.carikostkita.data.model.ChatConversation c : conversations) {
                        totalUnread += c.getUnreadCount();
                    }

                    int currentPosition = viewPager != null ? viewPager.getCurrentItem() : 0;
                    if (bottomNav != null && currentPosition != 3) {
                        bottomNav.setChatUnreadBadge(totalUnread > 0);
                    }
                    com.carikostkita.data.model.ChatConversation latest = conversations.get(0);
                    if (latest.getUnreadCount() > 0 && latest.getLastMessage() != null && !latest.getLastMessage().isEmpty()) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                            if (isFinishing() || isDestroyed()) return;
                            if (viewPager != null && viewPager.getCurrentItem() == 3) return;
                            String sender = sessionManager.isPemilikKost() ? latest.getNamaPencari() : latest.getNamaPemilik();
                            if (sender == null || sender.isEmpty()) sender = "Pemilik Kost";
                            com.carikostkita.util.NotificationBannerHelper.showMessageBanner(
                                    MainActivity.this,
                                    sender,
                                    latest.getLastMessage(),
                                    v -> navigateToChat()
                            );
                        }, 1200);
                    }
                }
            }
            @Override
            public void onError(String message) {}
        });
    }

    public void navigateToSearch() {
        if (viewPager != null) {
            viewPager.setCurrentItem(1, true);
        }
    }

    public void navigateToProfile() {
        if (viewPager != null) {
            viewPager.setCurrentItem(4, true);
        }
    }

    public void navigateToFavorites() {
        if (viewPager != null) {
            viewPager.setCurrentItem(2, true);
        }
    }

    public void navigateToChat() {
        if (viewPager != null) {
            viewPager.setCurrentItem(3, true);
        }
    }

    public void navigateToSearchWithCategory(com.carikostkita.data.model.TipeKost tipe) {
        if (viewPager != null) {
            viewPager.setCurrentItem(1, true);
        }
        Fragment f = getSupportFragmentManager().findFragmentByTag("f" + 1);
        if (f instanceof SearchFragment) {
            ((SearchFragment) f).applyCategoryFilter(tipe);
        }
    }

    // Adapter ViewPager2 untuk 5 tab utama: Beranda -> Cari -> Favorit -> Chat -> Profil
    private static class MainPagerAdapter extends FragmentStateAdapter {
        public MainPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 1:
                    return new SearchFragment();
                case 2:
                    return new FavoriteFragment();
                case 3:
                    return new ChatListFragment();
                case 4:
                    return new ProfileFragment();
                case 0:
                default:
                    return new HomeFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }
}
