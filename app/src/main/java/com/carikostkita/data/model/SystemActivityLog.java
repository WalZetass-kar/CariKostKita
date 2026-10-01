package com.carikostkita.data.model;

import java.io.Serializable;

public class SystemActivityLog implements Serializable {
    private int idLog;
    private int idUser;
    private String actorName;
    private String actionType;
    private String description;
    private String targetType;
    private int targetId;
    private String createdAt;

    public SystemActivityLog() {}

    public SystemActivityLog(int idUser, String actorName, String actionType, String description, String targetType, int targetId) {
        this.idUser = idUser;
        this.actorName = actorName;
        this.actionType = actionType;
        this.description = description;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public int getIdLog() {
        return idLog;
    }

    public void setIdLog(int idLog) {
        this.idLog = idLog;
    }

    public int getIdUser() {
        return idUser;
    }

    public void setIdUser(int idUser) {
        this.idUser = idUser;
    }

    public String getActorName() {
        return actorName != null ? actorName : "Sistem";
    }

    public String getUserName() {
        return getActorName();
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActionType() {
        return actionType != null ? actionType : "UMUM";
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public String getDescription() {
        return description != null ? description : "";
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetType() {
        return targetType != null ? targetType : "SISTEM";
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public int getTargetId() {
        return targetId;
    }

    public void setTargetId(int targetId) {
        this.targetId = targetId;
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
