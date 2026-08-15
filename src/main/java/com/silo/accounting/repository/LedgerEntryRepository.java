package com.silo.accounting.repository;

import com.silo.accounting.entity.LedgerEntry;
import com.silo.accounting.enums.EntryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByTransactionId(UUID transactionId);

    Page<LedgerEntry> findByAccountId(UUID accountId, Pageable pageable);

    @Query("select coalesce(sum(e.amount), 0) from LedgerEntry e where e.account.id = :accountId and e.entryType = :entryType")
    BigDecimal sumAmountByAccountIdAndEntryType(@Param("accountId") UUID accountId, @Param("entryType") EntryType entryType);
}
