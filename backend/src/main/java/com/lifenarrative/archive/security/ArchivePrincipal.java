package com.lifenarrative.archive.security;

public final class ArchivePrincipal {

    private final String userId;
    private final String username;
    private final String displayName;

    public ArchivePrincipal(String userId, String username, String displayName) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
    }

    public String userId() {
        return userId;
    }

    public String username() {
        return username;
    }

    public String displayName() {
        return displayName;
    }
}