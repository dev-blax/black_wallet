package com.james.wallet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    public enum EntryType { CREDIT, DEBIT }

    @Id
    // SEQUENCE (not IDENTITY) so Hibernate can pre-allocate ids in blocks and BATCH inserts.
    // allocationSize must match the DB sequence's increment (set to 50 in V4).
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ledger_entries_seq")
    @SequenceGenerator(name = "ledger_entries_seq", sequenceName = "ledger_entries_id_seq", allocationSize = 50)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 16)
    private EntryType entryType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "transfer_id")
    private UUID transferId;

    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    private OffsetDateTime createdAt;

    protected LedgerEntry() { }

    public LedgerEntry(Long accountId, EntryType entryType, BigDecimal amount, UUID transferId) {
        this.accountId = accountId;
        this.entryType = entryType;
        this.amount = amount;
        this.transferId = transferId;
    }

    public static LedgerEntry deposit(Long accountId, BigDecimal amount) {
        return new LedgerEntry(accountId, EntryType.CREDIT, amount, null);
    }

    public Long getId() { return id; }
    public Long getAccountId() { return accountId; }
    public EntryType getEntryType() { return entryType; }
    public BigDecimal getAmount() { return amount; }
    public UUID getTransferId() { return transferId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
