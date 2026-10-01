package com.carikostkita.data.local.dao;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.carikostkita.data.local.DatabaseHelper;
import com.carikostkita.data.model.SystemActivityLog;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {
    private final DatabaseHelper dbHelper;

    public ActivityLogDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long logAction(int idUser, String actorName, String actionType, String description, String targetType, int targetId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_user", idUser);
        values.put("actor_name", actorName);
        values.put("action_type", actionType);
        values.put("description", description);
        values.put("target_type", targetType);
        values.put("target_id", targetId);
        return db.insert("system_activity_log", null, values);
    }

    public List<SystemActivityLog> findAllLogs() {
        List<SystemActivityLog> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                "system_activity_log",
                null,
                null,
                null,
                null,
                null,
                "id_log DESC",
                "100" // Limit to last 100 entries for performance
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToLog(cursor));
            }
            cursor.close();
        }
        return list;
    }

    public int getTotalLogCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM system_activity_log", null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    private SystemActivityLog cursorToLog(Cursor cursor) {
        SystemActivityLog log = new SystemActivityLog();
        log.setIdLog(cursor.getInt(cursor.getColumnIndexOrThrow("id_log")));
        log.setIdUser(cursor.getInt(cursor.getColumnIndexOrThrow("id_user")));
        log.setActorName(cursor.getString(cursor.getColumnIndexOrThrow("actor_name")));
        log.setActionType(cursor.getString(cursor.getColumnIndexOrThrow("action_type")));
        log.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        log.setTargetType(cursor.getString(cursor.getColumnIndexOrThrow("target_type")));
        log.setTargetId(cursor.getInt(cursor.getColumnIndexOrThrow("target_id")));
        log.setCreatedAt(cursor.getString(cursor.getColumnIndexOrThrow("created_at")));
        return log;
    }
}
