package rs.edu.raf.banka1.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.edu.raf.banka1.dtos.LoanRequestDto;
import rs.edu.raf.banka1.model.LoanRequestStatus;
import rs.edu.raf.banka1.requests.CreateLoanRequest;
import rs.edu.raf.banka1.requests.StatusRequest;
import rs.edu.raf.banka1.services.LoanRequestService;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/loan/requests")
public class LoanRequestController {
    private final LoanRequestService loanRequestService;

    public LoanRequestController(
        final LoanRequestService loanRequestService
    ) {
        this.loanRequestService = loanRequestService;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get all loan requests", description = "Get all loan requests")
    @PreAuthorize("hasAuthority('manage_loan_requests')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful operation",
            content = {@Content(mediaType = "application/json", schema = @Schema(implementation = List.class))}),
        @ApiResponse(responseCode = "403",
            description = "You aren't authorized to get all loan requests"),
        @ApiResponse(responseCode = "404",
            description = "No loan requests found"),
        @ApiResponse(responseCode = "500",
            description = "Internal server error")
    })
    public ResponseEntity<List<LoanRequestDto>> getAllLoanRequests() {
        return ResponseEntity.ok(loanRequestService.getLoanRequests());
    }



    @PutMapping(value = "/{loanRequestId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "change loan request status", description = "change loan request status")
    @PreAuthorize("hasAuthority('manage_loan_requests')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful operation",
        content = {@Content(mediaType = "application/json",
            schema = @Schema(implementation = Boolean.class))}),
        @ApiResponse(responseCode = "403", description = "You aren't authorized to change status"),
        @ApiResponse(responseCode = "404", description = "Loan not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<Boolean> changeRequestStatus(
        @PathVariable(name = "loanRequestId") final Long id,
        @RequestBody final StatusRequest request
    ) {
        return ResponseEntity.ok(loanRequestService.changeRequestStatus(
            id,
            LoanRequestStatus.valueOf(request.getStatus())
        ));
    }

    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create loan request", description = "Create loan request")
    @PreAuthorize("hasAuthority('manage_loan_requests')")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Successful operation",
            content = {@Content(mediaType = "application/json",
                schema = @Schema(implementation = LoanRequestDto.class))}),
        @ApiResponse(responseCode = "403", description = "You aren't authorized to create loan request"),
        @ApiResponse(responseCode = "404", description = "Bad Request"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<LoanRequestDto> createLoanRequest(
        @RequestBody final CreateLoanRequest request
    ) {
        return ResponseEntity.ok(loanRequestService.createRequest(request));
    }
}
