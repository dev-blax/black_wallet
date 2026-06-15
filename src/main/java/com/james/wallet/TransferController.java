package com.james.wallet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/transfers")
@Validated
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping
    @PreAuthorize("@accountSecurity.isOwner(#request.sourceAccountId(), authentication)")
    public ResponseEntity<TransferResponse> create(
            @RequestHeader("Idempotency-Key")
            @NotBlank(message = "Idempotency-Key header is required")
            @Size(max = 64, message = "Idempotency-Key must be at most 64 characters")
            String idempotencyKey,
            @Valid @RequestBody CreateTransferRequest request) {

        Transfer transfer = transferService.transfer(
                request.sourceAccountId(),
                request.destinationAccountId(),
                request.amount(),
                idempotencyKey);

        return ResponseEntity.ok(TransferResponse.from(transfer));
    }
}
