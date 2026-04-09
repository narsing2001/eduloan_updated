package com.example.LoanTypeStrategyInterest.service.serv1;


import com.example.LoanTypeStrategyInterest.dto.dto1.*;
import com.example.LoanTypeStrategyInterest.entity.entity1.UserApplicationDetail;
import com.example.LoanTypeStrategyInterest.enums.enum1.*;
import com.example.LoanTypeStrategyInterest.exception.EmailAlradyExistsException;
import com.example.LoanTypeStrategyInterest.exception.LoanApplicationNotFoundException;
import com.example.LoanTypeStrategyInterest.exception.RequestAmountZeroException;
import com.example.LoanTypeStrategyInterest.factory.StrategyFactory;
import com.example.LoanTypeStrategyInterest.repository.repo1.UserApplicationRepository;
import com.example.LoanTypeStrategyInterest.strategy.InterestStrategy;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional

@Slf4j
public class UserApplicationDetailService {
    private static final BigDecimal FEMALE_DISCOUNT = new BigDecimal("0.50");


    private final UserApplicationRepository userApplicationRepository;
    private final StrategyFactory strategyFactory;
    public UserApplicationDetailService(StrategyFactory strategyFactory, UserApplicationRepository userApplicationRepository) {
        this.strategyFactory = strategyFactory;
        this.userApplicationRepository = userApplicationRepository;
    }

    @Transactional
    public UserApplicationResponseDTO applyLoan(UserApplicationRequestDTO requestDTO) {
        Optional<UserApplicationDetail> existing = userApplicationRepository.findByApplicantEmail(requestDTO.getApplicantEmail());
        if (existing.isPresent()) {
            throw new EmailAlradyExistsException("education loan with current application email is already exists");
        }
        validateRequest(requestDTO);

        InterestStrategy interestStrategy = strategyFactory.getStrategy(requestDTO.getLoanType());
        interestStrategy.validatePreConditions(requestDTO);

        LoanType supportedLoanType = interestStrategy.getSupportedLoanType(requestDTO);
        CoursesDomain selectedCourse = interestStrategy.getCourseType(requestDTO);
        log.info("Supported Loan Type: {}", supportedLoanType);
        log.info("Selected Course: {} - {}", selectedCourse.getDisplayName(), selectedCourse.getDescription());

        BigDecimal interestRate = interestStrategy.findInterestRate(requestDTO);

        boolean female_discount = (requestDTO.getGender() == Gender.FEMALE);
        if (female_discount) {
            interestRate = interestRate.subtract(FEMALE_DISCOUNT).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal maxAllowed = interestStrategy.getMaxLoanAmount(requestDTO);
        BigDecimal approvedAmt = requestDTO.getRequestedAmount().min(maxAllowed).setScale(2, RoundingMode.HALF_UP);
        int tenureYears = interestStrategy.getMaxTenureYears(requestDTO.getTenureYears());

        UserApplicationDetail applicationDetail = buildEntity(requestDTO, interestRate, approvedAmt, tenureYears);
        interestStrategy.totalCoverage(requestDTO, applicationDetail);
        applicationDetail.setMonthlyEmi(calculateEmi(approvedAmt, interestRate, tenureYears));
        applicationDetail.setApplicationStatus(ApplicationStatus.APPROVED);
        UserApplicationDetail savedEntity = userApplicationRepository.save(applicationDetail);
        return buildResponse(savedEntity, female_discount);
    }

    @Transactional(readOnly = true)
    public List<UserApplicationStatusResponseDTO> getByStatus(ApplicationStatus status) {
        return userApplicationRepository.findApplicantByApplicationStatus(status).stream().map(this::buildStatusResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserApplicationStatusResponseDTO> getByLoanType(LoanType loanType) {
        return userApplicationRepository.findByLoanType(loanType).stream().map(this::buildStatusResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<UserApplicationResponseDTO> getAll() {
        return userApplicationRepository.findAll().stream()
                .map(userApplicationDetail -> buildResponse(userApplicationDetail, userApplicationDetail.getGender() == Gender.FEMALE)).toList();
    }


     @Transactional
    public UserApplicationRejectResponseDTO evaluateApplication(UUID id) {
        UserApplicationDetail userApplicationDetail = userApplicationRepository.findById(id).orElseThrow(() -> new LoanApplicationNotFoundException(id));
         // Get the correct strategy based on loan type
         InterestStrategy interestStrategy = strategyFactory.getStrategy(userApplicationDetail.getLoanType());
         UserApplicationRequestDTO requestDTO = buildRequestDTO(userApplicationDetail);
        List<RejectionRule> rules = List.of(
                new RejectionRule(() -> !userApplicationDetail.isAdmissionLetterVerified(), RejectionReason.ADMISSION_NOT_VERIFIED),
                new RejectionRule(() -> !userApplicationDetail.isInstitutionApproved(), RejectionReason.INSTITUTION_NOT_APPROVED),
                new RejectionRule(() -> {
                                          UserApplicationRequestDTO userApplicationRequestDTO = buildRequestDTO(userApplicationDetail);
                                          BigDecimal maxAllowed = interestStrategy.getMaxLoanAmount(userApplicationRequestDTO);
                                                 return userApplicationDetail.getRequestedAmount().compareTo(maxAllowed) > 0;
                                     }, RejectionReason.AMOUNT_EXCEEDS_LIMIT),
                new RejectionRule(() -> {
                    Set<CourseType> eligibleCourses = Set.of(
                            CourseType.UNDERGRADUATE,
                            CourseType.POSTGRADUATE,
                            CourseType.DOCTORAL_PHD,
                            CourseType.VOCATIONAL_SKILL,
                            CourseType.PROFESSIONAL,
                            CourseType.MBA
                         );
                    return userApplicationDetail.getCourseType() == null || !eligibleCourses.contains(userApplicationDetail.getCourseType());
                                     }, RejectionReason.COURSE_NOT_ELIGIBLE)
        );

        //RejectionReason rejectionReason=rules.stream().filter(RejectionRule::test).map(RejectionRule::getReason).findFirst().orElse(RejectionReason.OTHER);
         List<RejectionReason> rejectionReason = rules.stream().filter(RejectionRule::test).map(RejectionRule::getReason).toList();
         if(rejectionReason.isEmpty()){
             userApplicationDetail.setApplicationStatus(ApplicationStatus.APPROVED);
             userApplicationRepository.save(userApplicationDetail);
             return buildRejectResponse(userApplicationDetail, rejectionReason);
         } else if (rejectionReason.size()==1) {
             userApplicationDetail.setApplicationStatus(ApplicationStatus.REJECTED);
             userApplicationRepository.save(userApplicationDetail);
             return buildRejectResponse(userApplicationDetail, rejectionReason);
         } else {
             userApplicationDetail.setApplicationStatus(ApplicationStatus.UNDER_REVIEW);
             userApplicationRepository.save(userApplicationDetail);
             return buildRejectResponse(userApplicationDetail, rejectionReason);
         }
     //    userApplicationDetail.setApplicationStatus(ApplicationStatus.REJECTED);
       // return getApplicationRejectResponseDTO(userApplicationDetail, rejectionReason);
      //   return buildRejectResponse(userApplicationDetail, rejectionReason);

    }

    private static UserApplicationRejectResponseDTO buildRejectResponse(UserApplicationDetail application, List<RejectionReason> failedReasons) {

        UserApplicationRejectResponseDTO userApplicationRejectResponseDTO = new UserApplicationRejectResponseDTO();
        userApplicationRejectResponseDTO.setApplicationId(application.getId());
        userApplicationRejectResponseDTO.setStatus(application.getApplicationStatus().name());
      //  userApplicationRejectResponseDTO.setRejectedBy("Admin LOAN_OFFICER");

        // Primary reason (first one) for code/message
//        RejectionReason primaryReason = failedReasons.isEmpty() ? RejectionReason.OTHER : failedReasons.get(0);
//        userApplicationRejectResponseDTO.setReasonCode(primaryReason.getCode());
//        userApplicationRejectResponseDTO.setMessage(primaryReason.getMessage());
        if(failedReasons.isEmpty()){

            userApplicationRejectResponseDTO.setApprovedBy("Admin LOAN_OFFICER");
            userApplicationRejectResponseDTO.setReasonCode(null);
            userApplicationRejectResponseDTO.setMessage("Application approved. All Eligibility criteria met");
            userApplicationRejectResponseDTO.setEligibilityCriteriaFailed(List.of());
        }else{
            RejectionReason primaryReason=failedReasons.getFirst();
            userApplicationRejectResponseDTO.setRejectedBy("Admin LOAN_OFFICER");
            userApplicationRejectResponseDTO.setReasonCode(primaryReason.getCode());
            userApplicationRejectResponseDTO.setMessage(primaryReason.getMessage());
            userApplicationRejectResponseDTO.setEligibilityCriteriaFailed(failedReasons.stream().map(RejectionReason::getMessage).toList());
        }


        // All failed criteria messages
        //userApplicationRejectResponseDTO.setEligibilityCriteriaFailed(failedReasons.isEmpty() ? List.of(RejectionReason.OTHER.getMessage()) : failedReasons.stream().map(RejectionReason::getMessage).toList());
        return userApplicationRejectResponseDTO;
    }



    private UserApplicationRequestDTO buildRequestDTO(UserApplicationDetail application) {
        return UserApplicationRequestDTO.builder()
                .applicantName(application.getApplicantName())
                .applicantEmail(application.getApplicantEmail())
                .gender(application.getGender())
                .loanType(application.getLoanType())
                .courseType(application.getCourseType())
                .requestedAmount(application.getRequestedAmount())
                .tenureYears(application.getTenureYears())
                .tuitionFeeCoverage(application.getTuitionFeeCoverage())
                .accommodationCoverage(application.getAccommodationCoverage())
                .travelExpenseCoverage(application.getTravelExpenseCoverage())
                .laptopExpenseCoverage(application.getLaptopExpenseCoverage())
                .examFeeCoverage(application.getExamFeeCoverage())
                .otherExpenseCoverage(application.getOtherExpenseCoverage())
                .admissionLetterVerified(application.isAdmissionLetterVerified())
                .institutionApproved(application.isInstitutionApproved())
                .build();
    }


    private UserApplicationStatusResponseDTO buildStatusResponse(UserApplicationDetail userApplicationDetail) {
        return UserApplicationStatusResponseDTO.builder()
                .applicationId(userApplicationDetail.getId().toString())
                .applicantName(userApplicationDetail.getApplicantName())
                .status(userApplicationDetail.getApplicationStatus())
                .loanType(userApplicationDetail.getLoanType())
                .admissionLetterVerified(userApplicationDetail.isAdmissionLetterVerified())
                .institutionApproved(userApplicationDetail.isInstitutionApproved())
                .build();
    }


    private BigDecimal calculateEmi(BigDecimal principal, BigDecimal annualRatePct, int years) {
        BigDecimal r = annualRatePct.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        int n = years * 12;
        BigDecimal power = BigDecimal.ONE.add(r).pow(n, new MathContext(10));
        return principal.multiply(r).multiply(power).divide(power.subtract(BigDecimal.ONE), 2, RoundingMode.HALF_UP);
    }

    private void validateRequest(UserApplicationRequestDTO requestDTO) {
        if (requestDTO.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RequestAmountZeroException("Requested amount must be greater than zero");
        }

    }

    private UserApplicationResponseDTO buildResponse(UserApplicationDetail userApplicationDetail, boolean femaleDiscount) {
        UserApplicationResponseDTO userApplicationResponseDTO = new UserApplicationResponseDTO();
        userApplicationResponseDTO.setId(userApplicationDetail.getId());
        userApplicationResponseDTO.setApplicantName(userApplicationDetail.getApplicantName());
        userApplicationResponseDTO.setApplicantEmail(userApplicationDetail.getApplicantEmail());
        userApplicationResponseDTO.setGender(userApplicationDetail.getGender());
        userApplicationResponseDTO.setLoanType(userApplicationDetail.getLoanType());
        userApplicationResponseDTO.setCourseType(userApplicationDetail.getCourseType());
        userApplicationResponseDTO.setDomesticCourseType(userApplicationDetail.getDomesticCourseType());
        userApplicationResponseDTO.setInternationalCourseType(userApplicationDetail.getInternationalCourseType());
        userApplicationResponseDTO.setRequestedAmount(userApplicationDetail.getRequestedAmount());
        userApplicationResponseDTO.setApprovedAmount(userApplicationDetail.getApprovedAmount());
        userApplicationResponseDTO.setInterestRate(userApplicationDetail.getInterestRate());
        userApplicationResponseDTO.setTenureYears(userApplicationDetail.getTenureYears());
        userApplicationResponseDTO.setMonthlyEmi(userApplicationDetail.getMonthlyEmi());
        userApplicationResponseDTO.setTuitionFeeCoverage(userApplicationDetail.getTuitionFeeCoverage());
        userApplicationResponseDTO.setAccommodationCoverage(userApplicationDetail.getAccommodationCoverage());
        userApplicationResponseDTO.setTravelExpenseCoverage(userApplicationDetail.getTravelExpenseCoverage());
        userApplicationResponseDTO.setLaptopExpenseCoverage(userApplicationDetail.getLaptopExpenseCoverage());
        userApplicationResponseDTO.setExamFeeCoverage(userApplicationDetail.getExamFeeCoverage());
        userApplicationResponseDTO.setOtherExpenseCoverage(userApplicationDetail.getOtherExpenseCoverage());
        userApplicationResponseDTO.setAdmissionLetterVerified(userApplicationDetail.isAdmissionLetterVerified());
        userApplicationResponseDTO.setInstitutionApproved(userApplicationDetail.isInstitutionApproved());
        userApplicationResponseDTO.setApplicationStatus(userApplicationDetail.getApplicationStatus());
        userApplicationResponseDTO.setCreatedAt(userApplicationDetail.getCreatedAt());
        userApplicationResponseDTO.setUpdatedAt(userApplicationDetail.getUpdatedAt());
        String message = femaleDiscount ? "Approved with special female candidate discount applied." : "Loan application approved successfully.";
        String applicantName= userApplicationResponseDTO.getApplicantName();
        UUID id= userApplicationResponseDTO.getId();
        log.info("Dear your loan application with id:{}, and  Name:{}, are {} ",id,applicantName,message);
        return userApplicationResponseDTO;
    }

    public UserApplicationStatusUpdateResponseDTO buildApplicationStatusResponse(UserApplicationDetail userApplicationDetail){
        UserApplicationStatusUpdateResponseDTO userApplicationStatusUpdateResponseDTO=new UserApplicationStatusUpdateResponseDTO();
        userApplicationStatusUpdateResponseDTO.setId(userApplicationDetail.getId());
        userApplicationStatusUpdateResponseDTO.setApplicationStatus(userApplicationDetail.getApplicationStatus());
        userApplicationStatusUpdateResponseDTO.setLoanType(userApplicationDetail.getLoanType());
        userApplicationStatusUpdateResponseDTO.setCourseType(userApplicationDetail.getCourseType());
        userApplicationStatusUpdateResponseDTO.setApplicantName(userApplicationDetail.getApplicantName());
        userApplicationStatusUpdateResponseDTO.setUpdatedAt(LocalDateTime.now());
        return userApplicationStatusUpdateResponseDTO;
    }

    public UserApplicationStatusUpdateResponseDTO updateStatus(UUID id, ApplicationStatus newStatus){
        UserApplicationDetail userApplicationDetail=userApplicationRepository.findById(id).orElseThrow(()->new LoanApplicationNotFoundException(id));
        userApplicationDetail.setApplicationStatus(newStatus);
        UserApplicationDetail updatedApplicantData=userApplicationRepository.save(userApplicationDetail);
        return buildApplicationStatusResponse(updatedApplicantData);
    }



    private UserApplicationDetail buildEntity(UserApplicationRequestDTO userApplicationRequestDTO, BigDecimal interestRate,
                                              BigDecimal approvedAmt, int tenureYears) {
        UserApplicationDetail applicationDetail = new UserApplicationDetail();
        applicationDetail.setApplicantName(userApplicationRequestDTO.getApplicantName());
        applicationDetail.setApplicantEmail(userApplicationRequestDTO.getApplicantEmail());
        applicationDetail.setGender(userApplicationRequestDTO.getGender());
        applicationDetail.setLoanType(userApplicationRequestDTO.getLoanType());
        applicationDetail.setCourseType(userApplicationRequestDTO.getCourseType());
        applicationDetail.setDomesticCourseType(userApplicationRequestDTO.getDomesticCourseType());
        applicationDetail.setInternationalCourseType(userApplicationRequestDTO.getInternationalCourseType());
        applicationDetail.setRequestedAmount(userApplicationRequestDTO.getRequestedAmount());
        applicationDetail.setInterestRate(interestRate);
        applicationDetail.setApprovedAmount(approvedAmt);
        applicationDetail.setTenureYears(tenureYears);
        applicationDetail.setTuitionFeeCoverage(userApplicationRequestDTO.getTuitionFeeCoverage());
        applicationDetail.setAccommodationCoverage(userApplicationRequestDTO.getAccommodationCoverage());
        applicationDetail.setTravelExpenseCoverage(userApplicationRequestDTO.getTravelExpenseCoverage());
        applicationDetail.setLaptopExpenseCoverage(userApplicationRequestDTO.getLaptopExpenseCoverage());
        applicationDetail.setExamFeeCoverage(userApplicationRequestDTO.getExamFeeCoverage());
        applicationDetail.setOtherExpenseCoverage(userApplicationRequestDTO.getOtherExpenseCoverage());
        applicationDetail.setAdmissionLetterVerified(userApplicationRequestDTO.isAdmissionLetterVerified());
        applicationDetail.setInstitutionApproved(userApplicationRequestDTO.isInstitutionApproved());
        return applicationDetail;
    }

    public UserApplicationResponseDTO updateStatus1(UUID id, ApplicationStatus newStatus) {
        UserApplicationDetail userApplicationDetail =userApplicationRepository.findById(id).orElseThrow(() -> new LoanApplicationNotFoundException(id));
        userApplicationDetail.setApplicationStatus(newStatus);
        return buildResponse(userApplicationRepository.save(userApplicationDetail), userApplicationDetail.getGender() == Gender.FEMALE);
    }

    private static UserApplicationRejectResponseDTO getApplicationRejectResponseDTO(UserApplicationDetail userApplicationDetail, RejectionReason rejectionReason) {
        UserApplicationRejectResponseDTO userApplicationRejectResponseDTO=new UserApplicationRejectResponseDTO();
        userApplicationRejectResponseDTO.setApplicationId(userApplicationDetail.getId());
        userApplicationRejectResponseDTO.setStatus(userApplicationDetail.getApplicationStatus().name());
        userApplicationRejectResponseDTO.setReasonCode(rejectionReason.getCode());
        userApplicationRejectResponseDTO.setMessage(rejectionReason.getMessage());
        userApplicationRejectResponseDTO.setRejectedBy("Admin LOAN_OFFICER");
        userApplicationRejectResponseDTO.setEligibilityCriteriaFailed(List.of(rejectionReason.getMessage()));
        return userApplicationRejectResponseDTO;
    }

    private UserApplicationRequestDTO buildRequestDTO1(UserApplicationDetail application) {
        return UserApplicationRequestDTO.builder()
                .applicantName(application.getApplicantName())
                .applicantEmail(application.getApplicantEmail())
                .gender(application.getGender())
                .loanType(application.getLoanType())
                .courseType(application.getCourseType())
                .requestedAmount(application.getRequestedAmount())
                .tenureYears(application.getTenureYears())
                .build();
    }

    private UserApplicationRejectResponseDTO buildRejectResponse1(UserApplicationDetail application, List<RejectionReason> failedReasons) {
        UserApplicationRejectResponseDTO userApplicationRejectResponseDTO = new UserApplicationRejectResponseDTO();
        userApplicationRejectResponseDTO.setApplicationId(application.getId());
        userApplicationRejectResponseDTO.setStatus(application.getApplicationStatus().name());
        userApplicationRejectResponseDTO.setReasonCode(failedReasons.isEmpty() ? RejectionReason.OTHER.getCode() : failedReasons.get(0).getCode());
        userApplicationRejectResponseDTO.setMessage(failedReasons.isEmpty() ? RejectionReason.OTHER.getMessage() : failedReasons.get(0).getMessage());
        userApplicationRejectResponseDTO.setRejectedBy(userApplicationRejectResponseDTO.getRejectedBy());
        userApplicationRejectResponseDTO.setEligibilityCriteriaFailed(failedReasons.stream().map(RejectionReason::getMessage).toList());
        return userApplicationRejectResponseDTO;
    }



}












