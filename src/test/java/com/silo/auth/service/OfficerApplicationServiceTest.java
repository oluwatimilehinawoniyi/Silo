package com.silo.auth.service;

import com.silo.auth.OfficerLookup;
import com.silo.auth.dto.OfficerApplicationResponse;
import com.silo.auth.entity.Credential;
import com.silo.auth.entity.OfficerApplication;
import com.silo.auth.entity.OfficerApplicationStatus;
import com.silo.auth.entity.Role;
import com.silo.auth.repository.CredentialRepository;
import com.silo.auth.repository.OfficerApplicationApprovalRepository;
import com.silo.auth.repository.OfficerApplicationRepository;
import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.DuplicateResourceException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.member.MemberLookup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfficerApplicationServiceTest {

    @Mock
    private OfficerApplicationRepository officerApplicationRepository;

    @Mock
    private OfficerApplicationApprovalRepository approvalRepository;

    @Mock
    private CredentialRepository credentialRepository;

    @Mock
    private OfficerLookup officerLookup;

    @Mock
    private MemberLookup memberLookup;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private OfficerApplicationService service;

    private static final UUID APPLICATION_ID = UUID.randomUUID();
    private static final UUID APPLICANT_ID = UUID.randomUUID();
    private static final UUID OFFICER_ONE = UUID.randomUUID();
    private static final UUID OFFICER_TWO = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new OfficerApplicationService(
                officerApplicationRepository, approvalRepository, credentialRepository, officerLookup,
                memberLookup, eventPublisher);
    }

    private Credential memberCredential() {
        return Credential.builder().memberId(APPLICANT_ID).role(Role.MEMBER).build();
    }

    private OfficerApplication pendingApplication() {
        return OfficerApplication.builder()
                .id(APPLICATION_ID).memberId(APPLICANT_ID).status(OfficerApplicationStatus.PENDING).build();
    }

    @Test
    @DisplayName("apply rejects a member with no credentials")
    void apply_throwsResourceNotFound_whenNoCredential() {
        when(credentialRepository.findByMemberId(APPLICANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.apply(APPLICANT_ID)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("apply rejects a member who is already an officer")
    void apply_throwsBusinessRuleViolation_whenAlreadyOfficer() {
        when(credentialRepository.findByMemberId(APPLICANT_ID))
                .thenReturn(Optional.of(Credential.builder().memberId(APPLICANT_ID).role(Role.OFFICER).build()));

        assertThatThrownBy(() -> service.apply(APPLICANT_ID)).isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("apply rejects a member with an existing pending application")
    void apply_throwsDuplicateResource_whenAlreadyPending() {
        when(credentialRepository.findByMemberId(APPLICANT_ID)).thenReturn(Optional.of(memberCredential()));
        when(officerApplicationRepository.existsByMemberIdAndStatus(APPLICANT_ID, OfficerApplicationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> service.apply(APPLICANT_ID)).isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    @DisplayName("apply creates a PENDING application and publishes the submitted event, when officers already exist")
    void apply_createsPendingApplication_whenOfficersAlreadyExist() {
        when(credentialRepository.findByMemberId(APPLICANT_ID)).thenReturn(Optional.of(memberCredential()));
        when(officerApplicationRepository.existsByMemberIdAndStatus(APPLICANT_ID, OfficerApplicationStatus.PENDING))
                .thenReturn(false);
        when(officerLookup.findAllOfficerMemberIds()).thenReturn(List.of(OFFICER_ONE));
        when(memberLookup.isActiveAndVerified(APPLICANT_ID)).thenReturn(true);
        when(officerApplicationRepository.save(any(OfficerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.apply(APPLICANT_ID);

        verify(eventPublisher).publishEvent(any(Object.class));
        verify(credentialRepository, never()).save(any());
    }

    @Test
    @DisplayName("apply rejects a member who isn't ACTIVE and KYC_VERIFIED, when officers already exist")
    void apply_throwsBusinessRuleViolation_whenNotEligibleAndOfficersExist() {
        when(credentialRepository.findByMemberId(APPLICANT_ID)).thenReturn(Optional.of(memberCredential()));
        when(officerApplicationRepository.existsByMemberIdAndStatus(APPLICANT_ID, OfficerApplicationStatus.PENDING))
                .thenReturn(false);
        when(officerLookup.findAllOfficerMemberIds()).thenReturn(List.of(OFFICER_ONE));
        when(memberLookup.isActiveAndVerified(APPLICANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.apply(APPLICANT_ID)).isInstanceOf(BusinessRuleViolationException.class);

        verify(officerApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("apply auto-approves and elevates the applicant when no officers exist yet")
    void apply_autoApprovesAndElevates_whenNoOfficersExistYet() {
        Credential credential = memberCredential();
        when(credentialRepository.findByMemberId(APPLICANT_ID)).thenReturn(Optional.of(credential));
        when(officerApplicationRepository.existsByMemberIdAndStatus(APPLICANT_ID, OfficerApplicationStatus.PENDING))
                .thenReturn(false);
        when(officerLookup.findAllOfficerMemberIds()).thenReturn(List.of());
        when(memberLookup.isActive(APPLICANT_ID)).thenReturn(true);
        when(officerApplicationRepository.save(any(OfficerApplication.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OfficerApplicationResponse response = service.apply(APPLICANT_ID);

        assertThat(response.status()).isEqualTo(OfficerApplicationStatus.APPROVED);
        assertThat(credential.getRole()).isEqualTo(Role.OFFICER);
        verify(credentialRepository).save(credential);
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("apply rejects a non-ACTIVE member even when no officers exist yet")
    void apply_throwsBusinessRuleViolation_whenNotActiveAndNoOfficersExistYet() {
        when(credentialRepository.findByMemberId(APPLICANT_ID)).thenReturn(Optional.of(memberCredential()));
        when(officerApplicationRepository.existsByMemberIdAndStatus(APPLICANT_ID, OfficerApplicationStatus.PENDING))
                .thenReturn(false);
        when(officerLookup.findAllOfficerMemberIds()).thenReturn(List.of());
        when(memberLookup.isActive(APPLICANT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.apply(APPLICANT_ID)).isInstanceOf(BusinessRuleViolationException.class);

        verify(officerApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve rejects an application that doesn't exist")
    void approve_throwsResourceNotFound_whenApplicationMissing() {
        when(officerApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(APPLICATION_ID, OFFICER_ONE))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("approve rejects an already-decided application")
    void approve_throwsBusinessRuleViolation_whenNotPending() {
        OfficerApplication decided = pendingApplication();
        decided.setStatus(OfficerApplicationStatus.APPROVED);
        when(officerApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(decided));

        assertThatThrownBy(() -> service.approve(APPLICATION_ID, OFFICER_ONE))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("approve rejects an officer approving the same application twice")
    void approve_throwsBusinessRuleViolation_whenOfficerAlreadyApproved() {
        when(officerApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApplication()));
        when(approvalRepository.existsByApplicationIdAndOfficerId(APPLICATION_ID, OFFICER_ONE)).thenReturn(true);

        assertThatThrownBy(() -> service.approve(APPLICATION_ID, OFFICER_ONE))
                .isInstanceOf(BusinessRuleViolationException.class);
    }

    @Test
    @DisplayName("approve records a first approval without elevating the applicant, when 2 officers already exist")
    void approve_firstApproval_doesNotElevate() {
        when(officerApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApplication()));
        when(approvalRepository.existsByApplicationIdAndOfficerId(APPLICATION_ID, OFFICER_ONE)).thenReturn(false);
        when(approvalRepository.countByApplicationId(APPLICATION_ID)).thenReturn(1L);
        when(officerLookup.findAllOfficerMemberIds()).thenReturn(List.of(OFFICER_ONE, OFFICER_TWO));

        service.approve(APPLICATION_ID, OFFICER_ONE);

        verify(credentialRepository, never()).save(any());
        verify(officerApplicationRepository, never()).save(any());
    }

    @Test
    @DisplayName("approve elevates the applicant once two distinct officers have approved")
    void approve_secondDistinctApproval_elevatesApplicant() {
        when(officerApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApplication()));
        when(approvalRepository.existsByApplicationIdAndOfficerId(APPLICATION_ID, OFFICER_TWO)).thenReturn(false);
        when(approvalRepository.countByApplicationId(APPLICATION_ID)).thenReturn(2L);
        when(officerLookup.findAllOfficerMemberIds()).thenReturn(List.of(OFFICER_ONE, OFFICER_TWO));
        when(credentialRepository.findByMemberId(APPLICANT_ID)).thenReturn(Optional.of(memberCredential()));

        service.approve(APPLICATION_ID, OFFICER_TWO);

        ArgumentCaptor<Credential> credentialCaptor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getRole()).isEqualTo(Role.OFFICER);

        ArgumentCaptor<OfficerApplication> applicationCaptor = ArgumentCaptor.forClass(OfficerApplication.class);
        verify(officerApplicationRepository).save(applicationCaptor.capture());
        assertThat(applicationCaptor.getValue().getStatus()).isEqualTo(OfficerApplicationStatus.APPROVED);
    }

    @Test
    @DisplayName("approve elevates with a single approval when only one officer currently exists")
    void approve_elevatesWithSingleApproval_whenOnlyOneOfficerExists() {
        when(officerApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApplication()));
        when(approvalRepository.existsByApplicationIdAndOfficerId(APPLICATION_ID, OFFICER_ONE)).thenReturn(false);
        when(approvalRepository.countByApplicationId(APPLICATION_ID)).thenReturn(1L);
        when(officerLookup.findAllOfficerMemberIds()).thenReturn(List.of(OFFICER_ONE));
        when(credentialRepository.findByMemberId(APPLICANT_ID)).thenReturn(Optional.of(memberCredential()));

        service.approve(APPLICATION_ID, OFFICER_ONE);

        ArgumentCaptor<Credential> credentialCaptor = ArgumentCaptor.forClass(Credential.class);
        verify(credentialRepository).save(credentialCaptor.capture());
        assertThat(credentialCaptor.getValue().getRole()).isEqualTo(Role.OFFICER);
    }

    @Test
    @DisplayName("reject marks a PENDING application REJECTED")
    void reject_marksApplicationRejected_whenPending() {
        when(officerApplicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(pendingApplication()));

        service.reject(APPLICATION_ID, OFFICER_ONE);

        ArgumentCaptor<OfficerApplication> captor = ArgumentCaptor.forClass(OfficerApplication.class);
        verify(officerApplicationRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(OfficerApplicationStatus.REJECTED);
        verify(credentialRepository, never()).save(any());
    }
}
