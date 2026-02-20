package com.prreview.app.enums;

import lombok.Getter;

@Getter
public enum ReviewStatus {

    PENDING("Pending", "badge-secondary"),
    APPROVED("Approved", "badge-success"),
    REJECTED("Rejected", "badge-danger"),
    CHANGES_REQUESTED("Changes Requested", "badge-warning");

    private final String displayName;
    private final String cssClass;

    ReviewStatus(String displayName, String cssClass) {
        this.displayName = displayName;
        this.cssClass = cssClass;
    }
}
