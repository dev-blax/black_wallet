package com.james.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    private UUID id;

    @Column(name = "source_account_id", nullable = false)
    private Long sourceAccountId;

    @Column(name = "destination_account_id", nullable = false)
    private Long destinationAccountId;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    protected Transfer() { }

    public Transfer(Long sourceAccountId, Long destinationAccountId, BigDecimal amount, String idempotencyKey) {
        this.id = UUID.randomUUID();
        this.sourceAccountId = sourceAccountId;
        this.destinationAccountId = destinationAccountId;
        this.amount = amount;
        this.idempotencyKey = idempotencyKey;
    }

    public UUID getId() { return id; }
    public Long getSourceAccountId() { return sourceAccountId; }
    public Long getDestinationAccountId() { return destinationAccountId; }
    public BigDecimal getAmount() { return amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
