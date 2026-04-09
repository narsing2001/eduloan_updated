package com.example.LoanTypeStrategyInterest.entity.entity1;

import com.example.LoanTypeStrategyInterest.enums.enum1.*;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name="User_Application_Details1")
@Getter
@Setter
public class UserApplicationDetail {
    /*
      @Id
      @GeneratedValue(strategy = GenerationType.AUTO)
      @Column(columnDefinition = "UUID",updatable = false,nullable = false)
      private UUID id;
    */
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "UUID")
    private UUID id;

    @Column(nullable = false)
    private String applicantName;

    @Column(nullable = false,unique = true)
    private String applicantEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseType courseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus applicationStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private DomesticCourseType domesticCourseType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private InternationalCourseType internationalCourseType;

    @Column(nullable = false,precision = 15,scale = 2)
    private BigDecimal requestedAmount;

    @Column(precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(precision = 15,scale = 2)
    private BigDecimal approvedAmount;

    @Column(nullable = false)
    @Max(value = 20) @Min(value = 1)
    private Integer tenureYears;

    @Column(precision = 10,scale = 2)
    private BigDecimal monthlyEmi;

    @Column(precision = 15,scale = 2)
    private BigDecimal tuitionFeeCoverage;

    @Column(precision = 15,scale = 2)
    private BigDecimal accommodationCoverage;

    @Column(precision = 15,scale = 2)
    private BigDecimal travelExpenseCoverage;

    @Column(precision = 15,scale = 2)
    private BigDecimal laptopExpenseCoverage;

    @Column(precision = 15,scale = 2)
    private BigDecimal examFeeCoverage;

    @Column(precision = 15,scale = 2)
    private BigDecimal otherExpenseCoverage;


    private boolean admissionLetterVerified;
    private boolean institutionApproved;

    @Column(nullable = false,updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;


    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        applicationStatus = ApplicationStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    @AssertTrue(message = "Either domesticCourseType or internationalCourseType must be set, but not both or neither")
    public boolean isValidCourseTypeSelection() {
        boolean hasDomestic = domesticCourseType != null;
        boolean hasInternational = internationalCourseType != null;
        return hasDomestic ^ hasInternational;
    }



}
