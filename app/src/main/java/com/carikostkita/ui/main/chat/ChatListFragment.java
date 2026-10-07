package com.carikostkita.ui.main.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.carikostkita.R;
import com.carikostkita.data.model.ChatConversation;
import com.carikostkita.data.remote.SupabaseRealtimeClient;
import com.carikostkita.data.repository.ChatRepository;
import com.carikostkita.data.repository.DataCallback;
import com.carikostkita.ui.adapter.ChatConversationAdapter;
import com.carikostkita.util.SessionManager;
import java.util.List;

public class ChatListFragment extends Fragment {

    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvConversations;
    private ProgressBar pbLoading;
    private LinearLayout layoutEmpty;

    private ChatRepository chatRepository;
    private SessionManager sessionManager;
    private ChatConversationAdapter adapter;
    private SupabaseRealtimeClient realtimeClient;

    private View layoutSkeleton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        chatRepository = new ChatRepository(requireContext());

        swipeRefresh = view.findViewById(R.id.swipe_chat_list);
        rvConversations = view.findViewById(R.id.rv_chat_conversations);
        pbLoading = view.findViewById(R.id.pb_chat_list);
        layoutSkeleton = view.findViewById(R.id.skeleton_chat_list);
        layoutEmpty = view.findViewById(R.id.layout_chat_list_empty);

        tvEmptyTitle = view.findViewById(R.id.tv_chat_empty_title);
        tvEmptyDesc = view.findViewById(R.id.tv_chat_empty_desc);
        ivEmptyIcon = view.findViewById(R.id.iv_chat_empty_icon);
        btnEmptyAction = view.findViewById(R.id.btn_chat_empty_cari_kost);

        com.carikostkita.util.PageHeader.bind(view, "Pesan", "Tanya langsung ke pemilik kost");
        swipeRefresh.setColorSchemeResources(R.color.primary);

        setupRecyclerView();
        setupRealtime();

        swipeRefresh.setOnRefreshListener(() -> loadConversations(false));
    }

    private TextView tvEmptyTitle;
    private TextView tvEmptyDesc;
    private android.widget.ImageView ivEmptyIcon;
    private TextView btnEmptyAction;
    private boolean hasLoadedOnce = false;

    private void showEmpty(String title, String desc, int icon, String action, View.OnClickListener onAction) {
        rvConversations.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
        if (tvEmptyTitle != null) tvEmptyTitle.setText(title);
        if (tvEmptyDesc != null) tvEmptyDesc.setText(desc);
        if (ivEmptyIcon != null) ivEmptyIcon.setImageResource(icon);
        if (btnEmptyAction != null) {
            btnEmptyAction.setText(action);
            btnEmptyAction.setOnClickListener(onAction);
        }
    }

    private void showNoConversations() {
        showEmpty("Belum Ada Percakapan",
                "Buka halaman kost yang kamu suka, lalu ketuk \"Chat Pemilik\" untuk bertanya soal kamar, harga, atau jadwal survei.",
                R.drawable.ic_nav_chat, "Cari Kost", v -> {
                    if (getActivity() instanceof com.carikostkita.ui.main.MainActivity) {
                        ((com.carikostkita.ui.main.MainActivity) getActivity()).navigateToSearch();
                    }
                });
    }

    private void setupRealtime() {
        realtimeClient = new SupabaseRealtimeClient();
        // Event realtime memuat ulang daftar tanpa skeleton agar tidak berkedip
        realtimeClient.setChatUpdateListener(() -> loadConversations(false));
        realtimeClient.setListener(msg -> {
            if (isAdded()) {
                loadConversations(false);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (realtimeClient != null && sessionManager.isLoggedIn()) {
            realtimeClient.connect(null);
        }
        loadConversations(!hasLoadedOnce);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (realtimeClient != null) {
            realtimeClient.disconnect();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (realtimeClient != null) {
            realtimeClient.disconnect();
        }
    }

    private void setupRecyclerView() {
        boolean isOwner = sessionManager.isPemilikKost();
        adapter = new ChatConversationAdapter(requireContext(), isOwner, conversation -> {
            Intent intent = new Intent(requireContext(), ChatRoomActivity.class);
            intent.putExtra("conversation_id", conversation.getIdConversation());
            intent.putExtra("kost_id", conversation.getIdKost());
            intent.putExtra("id_pemilik", conversation.getIdPemilik());
            intent.putExtra("id_pencari", conversation.getIdPencari());
            intent.putExtra("nama_kost", conversation.getNamaKost());
            intent.putExtra("foto_kost", conversation.getFotoKost());
            intent.putExtra("avatar_counterpart", conversation.getAvatarLawan());
            if (isOwner) {
                intent.putExtra("nama_counterpart", conversation.getNamaPencari());
            } else {
                intent.putExtra("nama_counterpart", conversation.getNamaPemilik());
            }
            startActivity(intent);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvConversations.setLayoutManager(layoutManager);
        rvConversations.setAdapter(adapter);
    }

    private void loadConversations(boolean showSkeleton) {
        String userUid = sessionManager.getUserUid();
        if (userUid == null || userUid.isEmpty()) {
            swipeRefresh.setRefreshing(false);
            showEmpty("Chat dengan Pemilik Kost",
                    "Masuk untuk bertanya langsung ke pemilik dan menyimpan riwayat percakapanmu.",
                    R.drawable.ic_nav_chat, "Masuk / Daftar",
                    v -> com.carikostkita.util.AuthPrompt.openLogin(requireContext()));
            return;
        }

        long startTime = com.carikostkita.util.SkeletonHelper.markStart();
        if (showSkeleton && !swipeRefresh.isRefreshing()) {
            if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.VISIBLE);
            if (pbLoading != null) pbLoading.setVisibility(View.GONE);
            rvConversations.setVisibility(View.GONE);
        }

        chatRepository.getConversationsForUser(userUid, new DataCallback<List<ChatConversation>>() {
            @Override
            public void onSuccess(List<ChatConversation> data) {
                com.carikostkita.util.SkeletonHelper.complete(startTime, () -> {
                    if (!isAdded()) return;
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    hasLoadedOnce = true;
                    adapter.setConversations(data);

                    if (data.isEmpty()) {
                        showNoConversations();
                    } else {
                        layoutEmpty.setVisibility(View.GONE);
                        rvConversations.setVisibility(View.VISIBLE);
                    }
                });
            }

            @Override
            public void onError(String message) {
                com.carikostkita.util.SkeletonHelper.complete(startTime, () -> {
                    if (!isAdded()) return;
                    if (layoutSkeleton != null) layoutSkeleton.setVisibility(View.GONE);
                    if (pbLoading != null) pbLoading.setVisibility(View.GONE);
                    swipeRefresh.setRefreshing(false);
                    if (adapter.getItemCount() > 0) {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    } else {
                        boolean offline = com.carikostkita.util.ErrorMessages.isOffline(message);
                        showEmpty(offline ? "Kamu Sedang Offline" : "Gagal Memuat Pesan", message,
                                offline ? R.drawable.ic_error_circle : R.drawable.ic_warning,
                                "Coba Lagi", v -> loadConversations(true));
                    }
                });
            }
        });
    }
}
