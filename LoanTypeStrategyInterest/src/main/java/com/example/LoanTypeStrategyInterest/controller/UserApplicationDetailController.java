package com.example.LoanTypeStrategyInterest.controller;


import com.example.LoanTypeStrategyInterest.dto.dto1.*;
import com.example.LoanTypeStrategyInterest.enums.enum1.ApplicationStatus;
import com.example.LoanTypeStrategyInterest.enums.enum1.LoanType;
import com.example.LoanTypeStrategyInterest.response.ApiResponse;
import com.example.LoanTypeStrategyInterest.service.serv1.InterestRateCalculationService;
import com.example.LoanTypeStrategyInterest.service.serv1.LoanInterestCalculationService;
import com.example.LoanTypeStrategyInterest.service.serv1.UserApplicationDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/loans")
public class UserApplicationDetailController {
    private final UserApplicationDetailService userApplicationDetailService;
    private  final InterestRateCalculationService interestRateCalculationService;
  //  private  final LoanInterestCalculationService loanInterestCalculationService;

    @PostMapping("/post/apply")
    public ResponseEntity<ApiResponse<UserApplicationResponseDTO>> submitUserApplication(@Valid @RequestBody UserApplicationRequestDTO userApplicationRequestDTO){
        UserApplicationResponseDTO applicant=userApplicationDetailService.applyLoan(userApplicationRequestDTO);
        ApiResponse<UserApplicationResponseDTO> response=ApiResponse.success(applicant,"Loan application submitted successfully...!");
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    @GetMapping("applicant/getAll")
    public ResponseEntity<ApiResponse<List<UserApplicationResponseDTO>>> getAllApplications() {
        List<UserApplicationResponseDTO> applications = userApplicationDetailService.getAll();
        ApiResponse<List<UserApplicationResponseDTO>> response = ApiResponse.success(applications, "Fetched all loan applications successfully!");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<UserApplicationStatusResponseDTO>>>getApplicantByStatus(@PathVariable("status")ApplicationStatus status){
        List<UserApplicationStatusResponseDTO> applicant=userApplicationDetailService.getByStatus(status);
        ApiResponse<List<UserApplicationStatusResponseDTO>> response = ApiResponse.success(applicant,"Fetched Loan application by status successfully..!");
        return  ResponseEntity.ok(response);
    }

    @GetMapping("/loanType/{loanType}")
    public ResponseEntity<ApiResponse<List<UserApplicationStatusResponseDTO>>> getApplicationByLoanType(@PathVariable("loanType")LoanType loanType){
        List<UserApplicationStatusResponseDTO> applicant =userApplicationDetailService.getByLoanType(loanType);
        ApiResponse<List<UserApplicationStatusResponseDTO>> response=ApiResponse.success(applicant,"Fetched Loan application by loanType successfully...!");
        return  ResponseEntity.ok(response);
    }

    @PatchMapping("/patch/updateStatus/{id}")
    public ResponseEntity<ApiResponse<UserApplicationStatusUpdateResponseDTO>> updateApplicationStatus(@RequestBody UpdateStatusRequest updateStatusRequest){
      UserApplicationStatusUpdateResponseDTO userApplicationStatusUpdateResponseDTO=userApplicationDetailService.updateStatus(updateStatusRequest.getId(),updateStatusRequest.getNewStatus());
      ApiResponse<UserApplicationStatusUpdateResponseDTO> response=ApiResponse.success(userApplicationStatusUpdateResponseDTO,"Application Status updated successfully...!");
      return ResponseEntity.ok(response);
    }

    @PutMapping("/application/{id}/evaluate")
    public ResponseEntity<UserApplicationRejectResponseDTO> rejectApplication(@PathVariable UUID id) {
        UserApplicationRejectResponseDTO response = userApplicationDetailService.evaluateApplication(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/cir")
    public ResponseEntity<ApiResponse<InterestRateCalculationResponseDTO>> calculateInterestRate(@Valid @RequestBody InterestRateCalculationRequestDTO request) {
        InterestRateCalculationResponseDTO result = interestRateCalculationService.calculateInterestRate(request);
        ApiResponse<InterestRateCalculationResponseDTO> response = ApiResponse.success(result, "Interest rate calculated successfully using " + result.getCalculationMethod());
        return ResponseEntity.ok(response);
    }


    @PostMapping("/cir/bs")
    public ResponseEntity<ApiResponse<InterestRateCalculationResponseDTO>> calculateInterestRateBinarySearch(@Valid @RequestBody InterestRateCalculationRequestDTO request) {
        InterestRateCalculationResponseDTO result = interestRateCalculationService.calculateInterestRateBinarySearch(request);
        ApiResponse<InterestRateCalculationResponseDTO> response = ApiResponse.success(result, "Interest rate calculated successfully using " + result.getCalculationMethod());
        return ResponseEntity.ok(response);
    }

//    @PostMapping("/ce")
//    public ResponseEntity<InterestRateCalculationResponseDTO> calculateEmiRateOfInterest(@RequestBody UserApplicationRequestDTO requestDTO) {
//        InterestRateCalculationResponseDTO response = loanInterestCalculationService.computeLoanInterestRate(requestDTO);
//        return ResponseEntity.ok(response);
//    }


}
