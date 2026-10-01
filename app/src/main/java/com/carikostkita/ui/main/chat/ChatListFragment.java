package com.carikostkita.ui.main.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.carikostkita.R;
import com.carikostkita.data.model.ChatConversation;
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
        layoutEmpty = view.findViewById(R.id.layout_chat_list_empty);

        setupRecyclerView();

        swipeRefresh.setOnRefreshListener(this::loadConversations);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadConversations();
    }

    private void setupRecyclerView() {
        boolean isOwner = sessionManager.isPemilikKost();
        adapter = new ChatConversationAdapter(requireContext(), isOwner, conversation -> {
            Intent intent = new Intent(requireContext(), ChatRoomActivity.class);
            intent.putExtra("conversation_id", conversation.getIdConversation());
            intent.putExtra("kost_id", conversation.getIdKost());
            intent.putExtra("id_pemilik", conversation.getIdPemilik());
            intent.putExtra("nama_kost", conversation.getNamaKost());
            intent.putExtra("foto_kost", conversation.getFotoKost());
            if (isOwner) {
                intent.putExtra("nama_counterpart", conversation.getNamaPencari());
            } else {
                intent.putExtra("nama_counterpart", conversation.getNamaPemilik());
            }
            startActivity(intent);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(requireContext());
        rvConversations.setLayoutManager(layoutManager);
        rvConversations.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));
        rvConversations.setAdapter(adapter);
    }

    private void loadConversations() {
        int userId = sessionManager.getUserId();
        if (userId <= 0) {
            swipeRefresh.setRefreshing(false);
            layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        if (!swipeRefresh.isRefreshing()) {
            pbLoading.setVisibility(View.VISIBLE);
        }

        chatRepository.getConversationsForUser(userId, new DataCallback<List<ChatConversation>>() {
            @Override
            public void onSuccess(List<ChatConversation> data) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                adapter.setConversations(data);

                if (data.isEmpty()) {
                    layoutEmpty.setVisibility(View.VISIBLE);
                    rvConversations.setVisibility(View.GONE);
                } else {
                    layoutEmpty.setVisibility(View.GONE);
                    rvConversations.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onError(String message) {
                if (!isAdded()) return;
                pbLoading.setVisibility(View.GONE);
                swipeRefresh.setRefreshing(false);
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
