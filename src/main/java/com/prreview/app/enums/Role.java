package com.prreview.app.enums;

import lombok.Getter;

@Getter
public enum Role {

    ADMIN("Administrator"),
    TEAM_LEAD("Team Lead"),
    REVIEWER("Reviewer"),
    DEVELOPER("Developer");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }
}
