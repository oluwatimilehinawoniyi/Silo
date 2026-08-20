package com.silo.auth.service;

import com.silo.auth.OfficerLookup;
import com.silo.auth.dto.OfficerApplicationResponse;
import com.silo.auth.entity.Credential;
import com.silo.auth.entity.OfficerApplication;
import com.silo.auth.entity.OfficerApplicationApproval;
import com.silo.auth.entity.OfficerApplicationStatus;
import com.silo.auth.entity.Role;
import com.silo.auth.event.OfficerApplicationSubmittedEvent;
import com.silo.auth.repository.CredentialRepository;
import com.silo.auth.repository.OfficerApplicationApprovalRepository;
import com.silo.auth.repository.OfficerApplicationRepository;
import com.silo.common.exception.BusinessRuleViolationException;
import com.silo.common.exception.DuplicateResourceException;
import com.silo.common.exception.ResourceNotFoundException;
import com.silo.member.MemberLookup;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Elevating a member to OFFICER normally needs two distinct existing
 * officers to approve - but that rule can't bootstrap a brand new
 * deployment, where zero officers exist to approve anyone. The required
 * approval count instead scales with how many officers currently exist
 * (capped at 2): the very first applicant needs zero approvals since
 * nobody could possibly give one, the second needs the lone officer's
 * approval, and from the third applicant onward the real two-officer rule
 * applies. No seed data or manual database step needed to go live.
 */
@Service
@RequiredArgsConstructor
public class OfficerApplicationService {

    private static final int MAX_APPROVALS_REQUIRED = 2;

    private final OfficerApplicationRepository officerApplicationRepository;
    private final OfficerApplicationApprovalRepository approvalRepository;
    private final CredentialRepository credentialRepository;
    private final OfficerLookup officerLookup;
    private final MemberLookup memberLookup;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public OfficerApplicationResponse apply(UUID memberId) {
        Credential credential = credentialRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("No credentials registered for this member"));

        if (credential.getRole() == Role.OFFICER) {
            throw new BusinessRuleViolationException("This member is already an officer");
        }

        if (officerApplicationRepository.existsByMemberIdAndStatus(memberId, OfficerApplicationStatus.PENDING)) {
            throw new DuplicateResourceException("This member already has a pending officer application");
        }

        boolean noOfficersExistYet = officerLookup.findAllOfficerMemberIds().isEmpty();

        // KYC verification itself requires an existing officer, so the very first officer can only be
        // held to an ACTIVE-membership standard - everyone after that must be ACTIVE and KYC_VERIFIED.
        boolean eligible = noOfficersExistYet
                ? memberLookup.isActive(memberId)
                : memberLookup.isActiveAndVerified(memberId);
        if (!eligible) {
            throw new BusinessRuleViolationException(
                    noOfficersExistYet
                            ? "Your account must be active before you can apply for officer status"
                            : "You need to complete KYC verification before you can apply for officer status");
        }

        OfficerApplication application = officerApplicationRepository.save(OfficerApplication.builder()
                .memberId(memberId)
                .status(noOfficersExistYet ? OfficerApplicationStatus.APPROVED : OfficerApplicationStatus.PENDING)
                .decidedAt(noOfficersExistYet ? LocalDateTime.now() : null)
                .build());

        if (noOfficersExistYet) {
            credential.setRole(Role.OFFICER);
            credentialRepository.save(credential);
        } else {
            eventPublisher.publishEvent(new OfficerApplicationSubmittedEvent(application.getId(), memberId));
        }

        return toResponse(application, memberId);
    }

    @Transactional
    public OfficerApplicationResponse approve(UUID applicationId, UUID approvingOfficerId) {
        OfficerApplication application = requirePending(applicationId);

        if (approvalRepository.existsByApplicationIdAndOfficerId(applicationId, approvingOfficerId)) {
            throw new BusinessRuleViolationException("You have already approved this application");
        }

        approvalRepository.save(OfficerApplicationApproval.builder()
                .applicationId(applicationId)
                .officerId(approvingOfficerId)
                .build());

        long approvalCount = approvalRepository.countByApplicationId(applicationId);
        int approvalsRequired = Math.min(MAX_APPROVALS_REQUIRED, officerLookup.findAllOfficerMemberIds().size());
        if (approvalCount >= approvalsRequired) {
            application.setStatus(OfficerApplicationStatus.APPROVED);
            application.setDecidedAt(LocalDateTime.now());
            officerApplicationRepository.save(application);

            Credential credential = credentialRepository.findByMemberId(application.getMemberId())
                    .orElseThrow(() -> new ResourceNotFoundException("No credentials registered for this member"));
            credential.setRole(Role.OFFICER);
            credentialRepository.save(credential);
        }

        return toResponse(application, approvingOfficerId);
    }

    @Transactional
    public OfficerApplicationResponse reject(UUID applicationId, UUID rejectingOfficerId) {
        OfficerApplication application = requirePending(applicationId);

        application.setStatus(OfficerApplicationStatus.REJECTED);
        application.setDecidedAt(LocalDateTime.now());
        officerApplicationRepository.save(application);

        return toResponse(application, rejectingOfficerId);
    }

    public List<OfficerApplicationResponse> listPending(UUID callerOfficerId) {
        return officerApplicationRepository.findByStatus(OfficerApplicationStatus.PENDING).stream()
                .map(application -> toResponse(application, callerOfficerId))
                .toList();
    }

    public List<OfficerApplicationResponse> listMine(UUID memberId) {
        return officerApplicationRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream()
                .map(application -> toResponse(application, memberId))
                .toList();
    }

    private OfficerApplication requirePending(UUID applicationId) {
        OfficerApplication application = officerApplicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Officer application not found with id " + applicationId));

        if (application.getStatus() != OfficerApplicationStatus.PENDING) {
            throw new BusinessRuleViolationException("This officer application has already been decided");
        }

        return application;
    }

    private OfficerApplicationResponse toResponse(OfficerApplication application, UUID callerId) {
        int approvalCount = (int) approvalRepository.countByApplicationId(application.getId());
        boolean approvedByCaller = approvalRepository.existsByApplicationIdAndOfficerId(application.getId(), callerId);
        return new OfficerApplicationResponse(
                application.getId(), application.getMemberId(), application.getStatus(),
                approvalCount, approvedByCaller, application.getCreatedAt(), application.getDecidedAt());
    }
}
