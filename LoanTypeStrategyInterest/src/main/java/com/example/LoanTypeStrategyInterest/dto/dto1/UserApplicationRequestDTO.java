package com.example.LoanTypeStrategyInterest.dto.dto1;

import com.example.LoanTypeStrategyInterest.entity.entity1.UserApplicationDetail;
import com.example.LoanTypeStrategyInterest.enums.enum1.*;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.Column;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserApplicationRequestDTO {

    private UUID id;
    @NotBlank(message = "Applicant name is required")
    private String applicantName;

    @NotBlank(message = "Applicant email is required")
    @Email(message = "Invalid email format")
    private String applicantEmail;

    @NotNull(message = "Gender is required")
    private Gender gender;

    @NotNull(message = "Loan type is required")
    private LoanType loanType;

    @NotNull(message = "Course type is required")
    private CourseType courseType;


    private DomesticCourseType domesticCourseType;
    private InternationalCourseType internationalCourseType;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "100000.00", message = "Requested amount must be at least 100000")
    private BigDecimal requestedAmount;

    @NotNull(message = "Tenure years is required")
    @Min(value = 1, message = "Tenure must be at least 1 year")
    @Max(value = 15, message = "Tenure cannot exceed 15 years")
    private Integer tenureYears;

    @DecimalMin(value = "0.00", message = "Coverage cannot be negative")
    private BigDecimal tuitionFeeCoverage;

    @DecimalMin(value = "0.00", message = "Coverage cannot be negative")
    private BigDecimal accommodationCoverage;

    @DecimalMin(value = "0.00", message = "Coverage cannot be negative")
    private BigDecimal travelExpenseCoverage;

    @DecimalMin(value = "0.00", message = "Coverage cannot be negative")
    private BigDecimal laptopExpenseCoverage;

    @DecimalMin(value = "0.00", message = "Coverage cannot be negative")
    private BigDecimal examFeeCoverage;

    @DecimalMin(value = "0.00", message = "Coverage cannot be negative")
    private BigDecimal otherExpenseCoverage;

    //@NotNull(message = "Admission letter verification status is required")
    private boolean admissionLetterVerified;

    //@NotNull(message = "Institution approval status is required")
    private boolean institutionApproved;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @AssertTrue(message = "For DOMESTIC loans, only domesticCourseType must be set. For INTERNATIONAL loans, only internationalCourseType must be set.")
    public boolean isValidCourseSelection() {
        if (loanType == LoanType.DOMESTIC) {
            return domesticCourseType != null && internationalCourseType == null;
        } else if (loanType == LoanType.INTERNATIONAL) {
            return internationalCourseType != null && domesticCourseType == null;
        }
        return false;
    }


    public static UserApplicationDetail toRequestDTO(UserApplicationRequestDTO dto) {
        UserApplicationDetail entity = new UserApplicationDetail();
        entity.setApplicantName(dto.getApplicantName());
        entity.setApplicantEmail(dto.getApplicantEmail());
        entity.setGender(dto.getGender());
        entity.setLoanType(dto.getLoanType());
        entity.setDomesticCourseType(dto.getDomesticCourseType());
        entity.setInternationalCourseType(dto.getInternationalCourseType());
        entity.setRequestedAmount(dto.getRequestedAmount());
        entity.setTenureYears(dto.getTenureYears());
        entity.setTuitionFeeCoverage(dto.getTuitionFeeCoverage());
        entity.setAccommodationCoverage(dto.getAccommodationCoverage());
        entity.setTravelExpenseCoverage(dto.getTravelExpenseCoverage());
        entity.setLaptopExpenseCoverage(dto.getLaptopExpenseCoverage());
        entity.setExamFeeCoverage(dto.getExamFeeCoverage());
        entity.setOtherExpenseCoverage(dto.getOtherExpenseCoverage());
        entity.setAdmissionLetterVerified(dto.isAdmissionLetterVerified());
        entity.setInstitutionApproved(dto.isInstitutionApproved());
        return entity;
    }


}