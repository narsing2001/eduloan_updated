package com.example.LoanTypeStrategyInterest.strategy;

import com.example.LoanTypeStrategyInterest.dto.dto1.UserApplicationRequestDTO;
import com.example.LoanTypeStrategyInterest.entity.entity1.UserApplicationDetail;
import com.example.LoanTypeStrategyInterest.enums.enum1.CoursesDomain;
import com.example.LoanTypeStrategyInterest.enums.enum1.LoanType;
import com.example.LoanTypeStrategyInterest.exception.InvalidTenureYearException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service("domesticLoanStrategy")
public class DomesticLoanStrategy implements InterestStrategy {


    private static final BigDecimal MIN_RATE = new BigDecimal("9.00");
    private static final BigDecimal MAX_RATE = new BigDecimal("12.00");
    private static final int DOMESTIC_MAX_TENURE_YEARS=15;

    private static final BigDecimal DOMESTIC_TUITION_EXPENSE       = new BigDecimal("0.70");
    private static final BigDecimal DOMESTIC_ACCOMMODATION_EXPENSE = new BigDecimal("0.10");
    private static final BigDecimal DOMESTIC_TRAVEL_EXPENSE        = new BigDecimal("0.05");
    private static final BigDecimal DOMESTIC_LAPTOP_EXPENSE        = new BigDecimal("0.05");
    private static final BigDecimal DOMESTIC_EXAM_FEE_EXPENSE      = new BigDecimal("0.05");
    private static final BigDecimal DOMESTIC_OTHER_EXPENSE         = new BigDecimal("0.05");



    @Override
    public BigDecimal findInterestRate(UserApplicationRequestDTO requestDTO) {

        double randomRate= ThreadLocalRandom.current().nextDouble(MIN_RATE.doubleValue(),MAX_RATE.doubleValue()+0.01);
        BigDecimal rate=BigDecimal.valueOf(randomRate).setScale(2,RoundingMode.HALF_UP);

        /*
          if (requestDTO.getGender() == Gender.FEMALE) {
              rate = rate.subtract(FEMALE_DISCOUNT);
              if (rate.compareTo(MIN_RATE) < 0) {
                  rate = MIN_RATE; // clamp to minimum
              }
          }
        */
        return rate;
    }



    @Override
    public BigDecimal getMaxLoanAmount(UserApplicationRequestDTO requestDTO) {
        return switch (requestDTO.getCourseType()) {
            case UNDERGRADUATE         -> new BigDecimal("1000000");
            case POSTGRADUATE          -> new BigDecimal("2000000");
            case DOCTORAL_PHD          -> new BigDecimal("3000000");
            case VOCATIONAL_SKILL      -> new BigDecimal("500000");
            case PROFESSIONAL          -> new BigDecimal("5000000");
            case MBA                   -> new BigDecimal("1200000");
        };
    }

    @Override
    public int getMaxTenureYears(int TenureYears) {
        if(TenureYears>DOMESTIC_MAX_TENURE_YEARS){
            throw new InvalidTenureYearException("tenure year must be less than or equalTo: "+DOMESTIC_MAX_TENURE_YEARS+" for the Domestic education Loan");
        }
        return TenureYears;
    }

    @Override
    public void validatePreConditions(UserApplicationRequestDTO requestDTO) {
//        if (!requestDTO.isInstitutionApproved()) {
//            throw new InstituteNotApprovedException("Institution must be UGC/AICTE approved");
//        }
//        if(!requestDTO.isAdmissionLetterVerified()){
//            throw new AdmissionLetterVerificationException("your admission letter for domestic education loan verification is pending");
//        }
        log.info("Dear Applicant, Domestic Education Loan for ApplicationId:{}, InstituteApprovedStatus:{}, AdmissionLetterVerified:{}",requestDTO.getId(),requestDTO.isInstitutionApproved(),requestDTO.isAdmissionLetterVerified());

    }

    @Override
    public void totalCoverage(UserApplicationRequestDTO requestDTO, UserApplicationDetail userApplicationDetail) {
     BigDecimal baseAmount=userApplicationDetail.getApprovedAmount();
     userApplicationDetail.setTuitionFeeCoverage(Ratio(requestDTO.getTuitionFeeCoverage(),baseAmount,DOMESTIC_TUITION_EXPENSE));
        userApplicationDetail.setAccommodationCoverage(Ratio(requestDTO.getAccommodationCoverage(),baseAmount,DOMESTIC_ACCOMMODATION_EXPENSE));
        userApplicationDetail.setTravelExpenseCoverage(Ratio(requestDTO.getTravelExpenseCoverage(),baseAmount,DOMESTIC_TRAVEL_EXPENSE));
        userApplicationDetail.setLaptopExpenseCoverage(Ratio(requestDTO.getLaptopExpenseCoverage(),baseAmount,DOMESTIC_LAPTOP_EXPENSE));
        userApplicationDetail.setExamFeeCoverage(Ratio(requestDTO.getExamFeeCoverage(),baseAmount,DOMESTIC_EXAM_FEE_EXPENSE));
        userApplicationDetail.setOtherExpenseCoverage(Ratio(requestDTO.getOtherExpenseCoverage(),baseAmount,DOMESTIC_OTHER_EXPENSE));
    }

    @Override
    public LoanType getSupportedLoanType(UserApplicationRequestDTO requestDTO) {
        return LoanType.DOMESTIC;
    }

    @Override
    public CoursesDomain getCourseType(UserApplicationRequestDTO requestDTO) {
        return requestDTO.getDomesticCourseType();
    }


    private BigDecimal Ratio(BigDecimal expenses,BigDecimal baseAmt,BigDecimal topicExpenseRate){
        if(expenses!=null && expenses.compareTo(BigDecimal.ZERO)>0){
            return expenses.setScale(2, RoundingMode.HALF_UP);
        }
        return baseAmt.multiply(topicExpenseRate).setScale(2,RoundingMode.HALF_UP);
    }
}






























































































/*


// Course-specific interest rate ranges
    private static final Map<CourseType, InterestRateRange> COURSE_INTEREST_RATES = new HashMap<>();
    private static final Map<DomesticCourseType, InterestRateRange> DOMAIN_COURSE_INTEREST_RATES = new HashMap<>();

    static {
        // Undergraduate: 9-10%
        COURSE_INTEREST_RATES.put(CourseType.UNDERGRADUATE, new InterestRateRange(new BigDecimal("9.00"), new BigDecimal("12.00")));

        // Postgraduate: 9.5-11%
        COURSE_INTEREST_RATES.put(CourseType.POSTGRADUATE, new InterestRateRange(new BigDecimal("9.00"), new BigDecimal("12.00")));

        // Doctoral/PhD: 9-9.5%
        COURSE_INTEREST_RATES.put(CourseType.DOCTORAL_PHD, new InterestRateRange(new BigDecimal("9.00"), new BigDecimal("12.00")));

        // MBA: 9.5-10.5%
        COURSE_INTEREST_RATES.put(CourseType.MBA, new InterestRateRange(new BigDecimal("9.00"), new BigDecimal("12.00")));

        // Default for VOCATIONAL_SKILL and PROFESSIONAL
        COURSE_INTEREST_RATES.put(CourseType.VOCATIONAL_SKILL, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("11.50")));
        COURSE_INTEREST_RATES.put(CourseType.PROFESSIONAL, new InterestRateRange(new BigDecimal("9.50"), new BigDecimal("11.00")));

        // Professional Courses - Premium (9.5-10%)
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.CHARTERED_ACCOUNTANCY, new InterestRateRange(new BigDecimal("9.50"), new BigDecimal("10.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.MEDICAL_MBBS, new InterestRateRange(new BigDecimal("9.50"), new BigDecimal("10.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.DENTAL_BDS, new InterestRateRange(new BigDecimal("9.50"), new BigDecimal("10.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.LAW_NLU, new InterestRateRange(new BigDecimal("9.50"), new BigDecimal("10.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.ARCHITECTURE_PREMIUM, new InterestRateRange(new BigDecimal("9.50"), new BigDecimal("10.00")));

        // Professional Courses - Standard (10-10.5%)
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.COMPANY_SECRETARY, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("10.50")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.COST_ACCOUNTANT, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("10.50")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.NURSING, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("10.50")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.PHARMACY, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("10.50")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.ENGINEERING_TIER2, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("10.50")));

        // Professional Courses - Certifications (10.5-11%)
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.ACCA, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.CFA, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.CPA_PREP, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.ACTUARIAL_SCIENCE, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.PROFESSIONAL_DIPLOMA, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));

        // Vocational - Government Certified (10-10.5%)
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.ITI_GOVT_CERTIFIED, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("10.50")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.NSDC_CERTIFIED, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("10.50")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.SKILL_COUNCIL_APPROVED, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("10.50")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.SKILL_INDIA_PROGRAM, new InterestRateRange(new BigDecimal("10.00"), new BigDecimal("10.50")));

        // Vocational - Private Skill Development (10.5-11%)
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.AWS_CERTIFICATION, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.GOOGLE_CERTIFICATION, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.MICROSOFT_CERTIFICATION, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.DIGITAL_MARKETING, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.CODING_BOOTCAMP, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.HEALTHCARE_ASSISTANT, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.BEAUTY_WELLNESS, new InterestRateRange(new BigDecimal("10.50"), new BigDecimal("11.00")));

        // Vocational - Short Term (11-11.5%)
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.SHORT_TERM_VOCATIONAL, new InterestRateRange(new BigDecimal("11.00"), new BigDecimal("11.50")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.APPRENTICESHIP, new InterestRateRange(new BigDecimal("11.00"), new BigDecimal("11.50")));
        DOMAIN_COURSE_INTEREST_RATES.put(DomesticCourseType.TRADE_SKILL_UPGRADE, new InterestRateRange(new BigDecimal("11.00"), new BigDecimal("11.50")));

    }


-------------------------------------------------------------------------------------------------------------------------------------------------------
    @Override
    public BigDecimal findInterestRate(UserApplicationRequestDTO requestDTO) {
        DomesticCourseType domesticCourseType = requestDTO.getDomesticCourseType();
        InterestRateRange range = (domesticCourseType != null && COURSE_INTEREST_RATES.containsKey(domesticCourseType))
                ? COURSE_INTEREST_RATES.get(domesticCourseType) : new InterestRateRange(MIN_RATE, MAX_RATE);

        double randomRate = ThreadLocalRandom.current().nextDouble(range.getMinRate().doubleValue(), range.getMaxRate().doubleValue() + 0.01);
        BigDecimal rate = BigDecimal.valueOf(randomRate).setScale(2, RoundingMode.HALF_UP);

        if (requestDTO.getGender() == Gender.FEMALE) {
           // rate = rate.subtract(FEMALE_DISCOUNT);
            if (rate.compareTo(range.getMinRate()) < 0) {
                rate = range.getMinRate(); // clamp to course-specific minimum
            }
        }
        return rate;
    }
-------------------------------------------------------------------------------------------------------------------------------------------------------------
    @Getter
    @RequiredArgsConstructor
    private static class InterestRateRange {
        private final BigDecimal minRate;
        private final BigDecimal maxRate;
    }
* */