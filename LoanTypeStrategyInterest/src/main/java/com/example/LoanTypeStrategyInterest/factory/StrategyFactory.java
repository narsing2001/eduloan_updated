package com.example.LoanTypeStrategyInterest.factory;

import com.example.LoanTypeStrategyInterest.enums.enum1.LoanType;
import com.example.LoanTypeStrategyInterest.exception.LoanStrategyNotSelectException;
import com.example.LoanTypeStrategyInterest.exception.UnsupportedLoanTypeSelectionException;
import com.example.LoanTypeStrategyInterest.strategy.DomesticLoanStrategy;
import com.example.LoanTypeStrategyInterest.strategy.InterestStrategy;
import com.example.LoanTypeStrategyInterest.strategy.InternationalLoanStrategy;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Getter
public class StrategyFactory {
    private final Map<LoanType, InterestStrategy> strategies;
    private final InterestStrategy domesticStrategy;
    private final InterestStrategy internationalStrategy;
    public StrategyFactory(Map<LoanType, InterestStrategy> strategies,
                           @Qualifier("domesticLoanStrategy") InterestStrategy domesticStrategy,
                           @Qualifier("internationalLoanStrategy") InterestStrategy internationalStrategy) {
        this.strategies = strategies;
        this.domesticStrategy = domesticStrategy;
        this.internationalStrategy = internationalStrategy;
    }
    public  InterestStrategy getStrategy(LoanType loanType) throws UnsupportedLoanTypeSelectionException {
        if (loanType == null) {
            throw new LoanStrategyNotSelectException("applicant not selected loan type, loan type empty selection");
        }
 /*
        return switch (loanType){
            case DOMESTIC -> new DomesticLoanStrategy();
            case INTERNATIONAL -> new InternationalLoanStrategy();
            default            -> throw new UnsupportedLoanTypeSelectionException("Unsupported loan type: must be DOMESTIC or INTERNATIONAL");
        };
  */

        return switch (loanType) {
            case DOMESTIC     -> domesticStrategy;
            case INTERNATIONAL -> internationalStrategy;
            default            -> throw new UnsupportedLoanTypeSelectionException("Unsupported loan type: must be DOMESTIC or INTERNATIONAL");
        };
    }


}
