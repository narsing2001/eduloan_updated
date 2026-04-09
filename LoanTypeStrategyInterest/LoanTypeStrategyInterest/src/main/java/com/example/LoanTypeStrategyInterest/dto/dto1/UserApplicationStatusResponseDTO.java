package com.example.LoanTypeStrategyInterest.dto.dto1;

import com.example.LoanTypeStrategyInterest.entity.entity1.UserApplicationDetail;
import com.example.LoanTypeStrategyInterest.enums.enum1.ApplicationStatus;
import com.example.LoanTypeStrategyInterest.enums.enum1.LoanType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserApplicationStatusResponseDTO extends UserApplicationResponseDTO {
    private String applicationId;
    private String applicantName;
    private ApplicationStatus status;
    private LoanType loanType;

    private UserApplicationStatusResponseDTO buildStatusResponse(UserApplicationDetail entity) {
        return UserApplicationStatusResponseDTO.builder()
                .applicationId(entity.getId().toString())
                .applicantName(entity.getApplicantName())
                .loanType(entity.getLoanType())
                .status(entity.getApplicationStatus())
                .admissionLetterVerified(entity.isAdmissionLetterVerified())
                .institutionApproved(entity.isInstitutionApproved())
                .build();
    }


}