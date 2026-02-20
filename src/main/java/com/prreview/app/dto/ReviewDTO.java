package com.prreview.app.dto;

import com.prreview.app.enums.ReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewDTO {

    private Long prId;

    private String comment;

    @NotNull(message = "Review status is required")
    private ReviewStatus status;
}
