package com.silo.loan.service;

import com.silo.common.exception.ResourceNotFoundException;
import com.silo.loan.dto.LoanDetailResponse;
import com.silo.loan.entity.Loan;
import com.silo.loan.entity.LoanInstallment;
import com.silo.loan.enums.InstallmentStatus;
import com.silo.loan.enums.LoanStatus;
import com.silo.loan.repository.LoanInstallmentRepository;
import com.silo.loan.repository.LoanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanQueryServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanInstallmentRepository loanInstallmentRepository;

    @InjectMocks
    private LoanQueryService loanQueryService;

    private static final UUID LOAN_ID = UUID.randomUUID();
    private static final UUID MEMBER_ID = UUID.randomUUID();

    @Test
    @DisplayName("getLoanDetail rejects a loan that doesn't exist")
    void getLoanDetail_throwsResourceNotFound_whenLoanMissing() {
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanQueryService.getLoanDetail(LOAN_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("getLoanDetail returns loan fields with its installment schedule, in order")
    void getLoanDetail_returnsLoanWithSchedule() {
        Loan loan = Loan.builder()
                .id(LOAN_ID)
                .loanRequestId(UUID.randomUUID())
                .memberId(MEMBER_ID)
                .principalAmount(new BigDecimal("50000.00"))
                .interestRate(new BigDecimal("12.5"))
                .durationMonths(12)
                .disbursedDate(LocalDateTime.now())
                .status(LoanStatus.ACTIVE)
                .outstandingBalance(new BigDecimal("53125.00"))
                .build();
        LoanInstallment first = LoanInstallment.builder()
                .id(UUID.randomUUID()).installmentNumber(1)
                .dueDate(LocalDateTime.now().plusMonths(1))
                .expectedAmount(new BigDecimal("4427.08"))
                .status(InstallmentStatus.PENDING)
                .build();
        LoanInstallment second = LoanInstallment.builder()
                .id(UUID.randomUUID()).installmentNumber(2)
                .dueDate(LocalDateTime.now().plusMonths(2))
                .expectedAmount(new BigDecimal("4427.08"))
                .status(InstallmentStatus.PENDING)
                .build();
        when(loanRepository.findById(LOAN_ID)).thenReturn(Optional.of(loan));
        when(loanInstallmentRepository.findByLoanIdOrderByInstallmentNumberAsc(LOAN_ID))
                .thenReturn(List.of(first, second));

        LoanDetailResponse response = loanQueryService.getLoanDetail(LOAN_ID);

        assertThat(response.id()).isEqualTo(LOAN_ID);
        assertThat(response.memberId()).isEqualTo(MEMBER_ID);
        assertThat(response.status()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(response.outstandingBalance()).isEqualTo(new BigDecimal("53125.00"));
        assertThat(response.installments()).hasSize(2);
        assertThat(response.installments().get(0).installmentNumber()).isEqualTo(1);
        assertThat(response.installments().get(1).installmentNumber()).isEqualTo(2);
    }
}
