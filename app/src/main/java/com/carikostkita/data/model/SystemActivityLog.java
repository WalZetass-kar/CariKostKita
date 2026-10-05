package com.carikostkita.data.model;

import java.io.Serializable;

public class SystemActivityLog implements Serializable {
    private String idLog;
    private String idUser;
    private String actorName;
    private String actionType;
    private String description;
    private String targetType;
    private String targetId;
    private String createdAt;

    public SystemActivityLog() {}

    public SystemActivityLog(String idUser, String actorName, String actionType, String description, String targetType, String targetId) {
        this.idUser = idUser;
        this.actorName = actorName;
        this.actionType = actionType;
        this.description = description;
        this.targetType = targetType;
        this.targetId = targetId;
    }

    public String getIdLog() {
        return idLog != null ? idLog : "";
    }

    public void setIdLog(String idLog) {
        this.idLog = idLog;
    }

    public String getIdUser() {
        return idUser != null ? idUser : "";
    }

    public void setIdUser(String idUser) {
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

    public String getTargetId() {
        return targetId != null ? targetId : "";
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getCreatedAt() {
        return createdAt != null ? createdAt : "";
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
