package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.ChatConversation;
import com.carikostkita.data.model.ChatMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatDAO {
    private final DatabaseHelper dbHelper;

    public ChatDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public ChatConversation getOrCreateConversation(int idKost, int idPencari, int idPemilik) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Check if existing
        String sql = "SELECT c.*, k.nama_kost, u1.nama as nama_pencari, u2.nama as nama_pemilik " +
                "FROM chat_conversation c " +
                "JOIN kost k ON c.id_kost = k.id_kost " +
                "JOIN users u1 ON c.id_pencari = u1.id_user " +
                "JOIN users u2 ON c.id_pemilik = u2.id_user " +
                "WHERE c.id_kost = ? AND c.id_pencari = ? AND c.id_pemilik = ?";

        Cursor cursor = db.rawQuery(sql, new String[]{
                String.valueOf(idKost), String.valueOf(idPencari), String.valueOf(idPemilik)
        });

        if (cursor != null && cursor.moveToFirst()) {
            ChatConversation conv = cursorToConversation(cursor);
            cursor.close();
            return conv;
        }
        if (cursor != null) cursor.close();

        // Create new
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
        ContentValues cv = new ContentValues();
        cv.put("id_kost", idKost);
        cv.put("id_pencari", idPencari);
        cv.put("id_pemilik", idPemilik);
        cv.put("last_message", "Percakapan dimulai");
        cv.put("last_message_time", timeStr);
        long newId = db.insert("chat_conversation", null, cv);

        return findConversationById((int) newId);
    }

    public ChatConversation findConversationById(int idConversation) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT c.*, k.nama_kost, u1.nama as nama_pencari, u2.nama as nama_pemilik " +
                "FROM chat_conversation c " +
                "JOIN kost k ON c.id_kost = k.id_kost " +
                "JOIN users u1 ON c.id_pencari = u1.id_user " +
                "JOIN users u2 ON c.id_pemilik = u2.id_user " +
                "WHERE c.id_conversation = ?";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idConversation)});
        ChatConversation conv = null;
        if (cursor != null && cursor.moveToFirst()) {
            conv = cursorToConversation(cursor);
            cursor.close();
        }
        return conv;
    }

    public List<ChatConversation> getConversationsForUser(int idUser) {
        List<ChatConversation> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT c.*, k.nama_kost, u1.nama as nama_pencari, u2.nama as nama_pemilik " +
                "FROM chat_conversation c " +
                "JOIN kost k ON c.id_kost = k.id_kost " +
                "JOIN users u1 ON c.id_pencari = u1.id_user " +
                "JOIN users u2 ON c.id_pemilik = u2.id_user " +
                "WHERE c.id_pencari = ? OR c.id_pemilik = ? " +
                "ORDER BY c.id_conversation DESC";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idUser), String.valueOf(idUser)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToConversation(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public List<ChatMessage> getMessages(int idConversation) {
        List<ChatMessage> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT m.*, u.nama as nama_sender FROM chat_message m " +
                "JOIN users u ON m.id_sender = u.id_user " +
                "WHERE m.id_conversation = ? " +
                "ORDER BY m.id_message ASC";

        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(idConversation)});
        if (cursor != null) {
            while (cursor.moveToNext()) {
                ChatMessage m = new ChatMessage();
                m.setIdMessage(cursor.getInt(cursor.getColumnIndexOrThrow("id_message")));
                m.setIdConversation(cursor.getInt(cursor.getColumnIndexOrThrow("id_conversation")));
                m.setIdSender(cursor.getInt(cursor.getColumnIndexOrThrow("id_sender")));
                m.setNamaSender(cursor.getString(cursor.getColumnIndexOrThrow("nama_sender")));
                m.setMessage(cursor.getString(cursor.getColumnIndexOrThrow("message")));
                m.setRead(cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) == 1);
                m.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
                list.add(m);
            }
            cursor.close();
        }
        return list;
    }

    public ChatMessage sendMessage(int idConversation, int idSender, String messageText) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String timeStr = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());

        ContentValues cvMsg = new ContentValues();
        cvMsg.put("id_conversation", idConversation);
        cvMsg.put("id_sender", idSender);
        cvMsg.put("message", messageText.trim());
        cvMsg.put("is_read", 0);
        long msgId = db.insert("chat_message", null, cvMsg);

        // Update conversation summary
        ContentValues cvConv = new ContentValues();
        cvConv.put("last_message", messageText.trim());
        cvConv.put("last_message_time", timeStr);
        db.update("chat_conversation", cvConv, "id_conversation = ?", new String[]{String.valueOf(idConversation)});

        ChatMessage msg = new ChatMessage();
        msg.setIdMessage((int) msgId);
        msg.setIdConversation(idConversation);
        msg.setIdSender(idSender);
        msg.setMessage(messageText.trim());
        msg.setCreatedAt(timeStr);
        msg.setRead(false);
        return msg;
    }

    public void markMessagesAsRead(int idConversation, int currentUserId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("is_read", 1);
        db.update("chat_message", cv, "id_conversation = ? AND id_sender != ?",
                new String[]{String.valueOf(idConversation), String.valueOf(currentUserId)});
    }

    private ChatConversation cursorToConversation(Cursor cursor) {
        ChatConversation c = new ChatConversation();
        c.setIdConversation(cursor.getInt(cursor.getColumnIndexOrThrow("id_conversation")));
        c.setIdKost(cursor.getInt(cursor.getColumnIndexOrThrow("id_kost")));
        c.setNamaKost(cursor.getString(cursor.getColumnIndexOrThrow("nama_kost")));
        c.setIdPencari(cursor.getInt(cursor.getColumnIndexOrThrow("id_pencari")));
        c.setNamaPencari(cursor.getString(cursor.getColumnIndexOrThrow("nama_pencari")));
        c.setIdPemilik(cursor.getInt(cursor.getColumnIndexOrThrow("id_pemilik")));
        c.setNamaPemilik(cursor.getString(cursor.getColumnIndexOrThrow("nama_pemilik")));
        c.setLastMessage(cursor.getString(cursor.getColumnIndexOrThrow("last_message")));
        c.setLastMessageTime(cursor.getString(cursor.getColumnIndexOrThrow("last_message_time")));
        return c;
    }
}
