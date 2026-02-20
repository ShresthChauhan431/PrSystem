package com.prreview.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PullRequestDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 255)
    private String title;

    @Size(max = 2000)
    private String description;

    @NotBlank(message = "Branch name is required")
    @Size(max = 255)
    private String branchName;

    @Size(max = 500)
    private String repoUrl;
}
