package com.example.LoanTypeStrategyInterest.globalexception;

import com.example.LoanTypeStrategyInterest.exception.*;
import com.example.LoanTypeStrategyInterest.response.ApiResponse;
import com.example.LoanTypeStrategyInterest.response.ErrorResponse;
import com.example.LoanTypeStrategyInterest.response.ErrorResponse1;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(InstituteNotApprovedException.class)
    public ResponseEntity<ApiResponse<Void>> handleInstituteNotApproved(InstituteNotApprovedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "INSTITUTE_NOT_APPROVED"));
    }


    @ExceptionHandler(InvalidTenureYearException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidTenureYear(InvalidTenureYearException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(ex.getMessage(), "INVALID_TENURE_YEAR"));
    }

    @ExceptionHandler(InvalidStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidStatus(InvalidStatusException ex) {
        ApiResponse<Void> errorResponse = ApiResponse.failure(
                "Invalid status value: " + ex.getInvalidStatus(),
                "INVALID_STATUS"
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse1> handleCustomValidationException(ValidationException ex) {
        ErrorResponse1 errorResponse1 = ErrorResponse1.builder()
                .success(false)
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .errorCode("VALIDATION_ERROR")
                .message(ex.getMessage())
                .validationErrors(ex.getValidationErrors())
                .build();

        log.error("VALIDATION_ERROR: {} - Errors: {}", ex.getMessage(), ex.getValidationErrors());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse1);
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationErrors(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage()).collect(Collectors.joining(", "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(errors, "VALIDATION_ERROR"));
    }



    @ExceptionHandler(AdmissionLetterVerificationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAdmissionLetterVerification(AdmissionLetterVerificationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ex.getMessage(), "ADMISSION_LETTER_VERIFICATION_FAILED"));
    }

    @ExceptionHandler(ApplicationNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleApplicationNotFoundException(ApplicationNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.failure(ex.getMessage(), "Applicant_Not_Found_"));

    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife && ife.getTargetType() != null && ife.getTargetType().isEnum()) {
            String invalidValue = String.valueOf(ife.getValue());
            String fieldName = ife.getPath().isEmpty() ? "unknown" : ife.getPath().get(ife.getPath().size() - 1).getFieldName();
            String acceptedValues = Arrays.stream(ife.getTargetType().getEnumConstants()).map(Object::toString).collect(Collectors.joining(", "));
            String message = String.format("Invalid value '%s' for field '%s'. Accepted values are: [%s]", invalidValue, fieldName, acceptedValues);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.failure(message, "INVALID_ENUM_VALUE"));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure("Malformed JSON request body", "INVALID_REQUEST_BODY"));
    }

    @ExceptionHandler(InterestComputationException.class)
    public ResponseEntity<ErrorResponse1> handleInterestComputationException(InterestComputationException ex) {
        return buildErrorResponse1(HttpStatus.UNPROCESSABLE_ENTITY, "INTEREST_COMPUTATION_FAILED", ex.getMessage());
    }

    @ExceptionHandler(InvalidLoanParameterException.class)
    public ResponseEntity<ErrorResponse1> handleInvalidLoanParameterException(InvalidLoanParameterException ex) {
        return buildErrorResponse1(HttpStatus.BAD_REQUEST, "INVALID_EMPTY_LOAN_PARAMETERS", ex.getMessage());
    }


    // Handle generic exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.failure("An unexpected error occurred: " + ex.getMessage(), "GENERIC_ERROR"));
    }

    private ResponseEntity<ErrorResponse1> buildErrorResponse1(HttpStatus status, String errorCode, String message) {
        ErrorResponse1 errorResponse1 = ErrorResponse1.builder()
                .success(false)
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .errorCode(errorCode)
                .message(message)
                .build();
        log.error("{}: {}", errorCode, message);
        return ResponseEntity.status(status).body(errorResponse1);
    }

}