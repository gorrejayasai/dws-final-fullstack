package com.cognizant.digitalwalletsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminReviewRequest {


     //Remarks are required for rejection (the user needs to know why it was rejected).
     //Remarks are optional for approval — admin can add a note if needed
    @Size(max = 500, message = "Remarks cannot exceed 500 characters")
    private String remarks;
}
