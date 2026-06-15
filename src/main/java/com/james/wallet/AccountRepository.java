package com.james.wallet;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    List<Account> findByUserId(Long userId);

    /**
     * Returns just the owner's user id for an account — a projection, not the whole entity.
     * Called on EVERY secured request by the @PreAuthorize ownership check, so we avoid hydrating
     * the Account (and its lazy User proxy) and select a single column instead.
     */
    @Query("select a.user.id from Account a where a.id = :id")
    Optional<Long> findOwnerId(Long id);
}
