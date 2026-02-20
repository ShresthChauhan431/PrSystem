package com.prreview.app.enums;

import lombok.Getter;

@Getter
public enum PRStatus {

    OPEN("Open", "badge-primary"),
    IN_REVIEW("In Review", "badge-warning"),
    APPROVED("Approved", "badge-success"),
    REJECTED("Rejected", "badge-danger"),
    MERGED("Merged", "badge-secondary");

    private final String displayName;
    private final String cssClass;

    PRStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }
}
