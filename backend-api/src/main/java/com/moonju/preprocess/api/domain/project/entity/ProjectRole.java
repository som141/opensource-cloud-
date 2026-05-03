package com.moonju.preprocess.api.domain.project.entity;

public enum ProjectRole {

    OWNER,
    EDITOR,
    VIEWER;

    public boolean canRead() {
        return true;
    }

    public boolean canEdit() {
        return this == OWNER || this == EDITOR;
    }

    public boolean canManageMembers() {
        return this == OWNER;
    }
}
