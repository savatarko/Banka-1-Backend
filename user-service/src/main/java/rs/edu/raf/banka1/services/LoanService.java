package rs.edu.raf.banka1.services;

import rs.edu.raf.banka1.dtos.LoanDto;
import rs.edu.raf.banka1.dtos.LoanFullDto;
import rs.edu.raf.banka1.requests.ForeignCurrencyAccountRequest;
import rs.edu.raf.banka1.responses.CreateForeignCurrencyAccountResponse;
import rs.edu.raf.banka1.responses.ForeignCurrencyAccountResponse;

import java.util.List;

public interface LoanService {

    List<LoanDto> getLoans();

    List<LoanDto> getLoansForUser(final Long userId);

    List<LoanDto> getLoansForAccount(final String accountNumber);

    LoanFullDto getLoanDetails(final Long id);

}
