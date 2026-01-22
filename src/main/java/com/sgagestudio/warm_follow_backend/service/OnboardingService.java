package com.sgagestudio.warm_follow_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sgagestudio.warm_follow_backend.dto.OnboardingStateResponse;
import com.sgagestudio.warm_follow_backend.dto.OnboardingStep;
import com.sgagestudio.warm_follow_backend.dto.OnboardingStepsResponse;
import com.sgagestudio.warm_follow_backend.dto.OnboardingUpdateRequest;
import com.sgagestudio.warm_follow_backend.model.OnboardingState;
import com.sgagestudio.warm_follow_backend.model.ReminderStatus;
import com.sgagestudio.warm_follow_backend.model.SendingDomainStatus;
import com.sgagestudio.warm_follow_backend.repository.OnboardingStateRepository;
import com.sgagestudio.warm_follow_backend.repository.ReminderRepository;
import com.sgagestudio.warm_follow_backend.repository.SendingDomainRepository;
import com.sgagestudio.warm_follow_backend.repository.TemplateRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class OnboardingService {
    private final OnboardingStateRepository onboardingStateRepository;
    private final WorkspaceContextService workspaceContextService;
    private final TemplateRepository templateRepository;
    private final ReminderRepository reminderRepository;
    private final SendingDomainRepository sendingDomainRepository;
    private final ObjectMapper objectMapper;

    public OnboardingService(
            OnboardingStateRepository onboardingStateRepository,
            WorkspaceContextService workspaceContextService,
            TemplateRepository templateRepository,
            ReminderRepository reminderRepository,
            SendingDomainRepository sendingDomainRepository,
            ObjectMapper objectMapper
    ) {
        this.onboardingStateRepository = onboardingStateRepository;
        this.workspaceContextService = workspaceContextService;
        this.templateRepository = templateRepository;
        this.reminderRepository = reminderRepository;
        this.sendingDomainRepository = sendingDomainRepository;
        this.objectMapper = objectMapper;
    }

    public OnboardingStateResponse getState() {
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        ObjectNode overrides = resolveOverrides(workspaceId);
        return buildResponse(workspaceId, overrides);
    }

    public OnboardingStateResponse update(OnboardingUpdateRequest request) {
        UUID workspaceId = workspaceContextService.requireContext().workspace().getId();
        OnboardingState state = onboardingStateRepository.findById(workspaceId)
                .orElseGet(() -> createState(workspaceId));
        ObjectNode overrides = state.getSteps() instanceof ObjectNode obj
                ? obj
                : objectMapper.createObjectNode();
        String stepKey = request.step().name();
        if (request.completed()) {
            overrides.put(stepKey, true);
        } else {
            overrides.remove(stepKey);
        }
        state.setSteps(overrides);
        OnboardingStateResponse response = buildResponse(workspaceId, overrides);
        state.setCurrentStep(response.current_step() != null ? response.current_step().name() : null);
        onboardingStateRepository.save(state);
        return response;
    }

    private OnboardingState createState(UUID workspaceId) {
        OnboardingState state = new OnboardingState();
        state.setWorkspaceId(workspaceId);
        state.setSteps(objectMapper.createObjectNode());
        return state;
    }

    private ObjectNode resolveOverrides(UUID workspaceId) {
        return onboardingStateRepository.findById(workspaceId)
                .map(state -> state.getSteps() instanceof ObjectNode obj ? obj : objectMapper.createObjectNode())
                .orElseGet(objectMapper::createObjectNode);
    }

    private OnboardingStateResponse buildResponse(UUID workspaceId, ObjectNode overrides) {
        boolean workspaceCreated = true;
        boolean domainConfigured = sendingDomainRepository.existsByWorkspaceIdAndStatus(workspaceId, SendingDomainStatus.verified);
        boolean templateCreated = templateRepository.existsByWorkspaceId(workspaceId);
        boolean reminderScheduled = reminderRepository.existsByWorkspaceIdAndStatus(workspaceId, ReminderStatus.active);

        boolean workspaceCreatedValue = applyOverride(workspaceCreated, overrides, OnboardingStep.workspace_created);
        boolean domainConfiguredValue = applyOverride(domainConfigured, overrides, OnboardingStep.domain_configured);
        boolean templateCreatedValue = applyOverride(templateCreated, overrides, OnboardingStep.template_created);
        boolean reminderScheduledValue = applyOverride(reminderScheduled, overrides, OnboardingStep.first_reminder_scheduled);

        OnboardingStepsResponse steps = new OnboardingStepsResponse(
                workspaceCreatedValue,
                domainConfiguredValue,
                templateCreatedValue,
                reminderScheduledValue
        );
        return new OnboardingStateResponse(steps, resolveCurrentStep(steps));
    }

    private boolean applyOverride(boolean computed, ObjectNode overrides, OnboardingStep step) {
        if (overrides != null && overrides.has(step.name()) && overrides.get(step.name()).asBoolean(false)) {
            return true;
        }
        return computed;
    }

    private OnboardingStep resolveCurrentStep(OnboardingStepsResponse steps) {
        if (!steps.workspace_created()) {
            return OnboardingStep.workspace_created;
        }
        if (!steps.domain_configured()) {
            return OnboardingStep.domain_configured;
        }
        if (!steps.template_created()) {
            return OnboardingStep.template_created;
        }
        if (!steps.first_reminder_scheduled()) {
            return OnboardingStep.first_reminder_scheduled;
        }
        return null;
    }
}
