package com.petcare.backend.domain.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

public record PlanResponse(
		String plan, String tier, String status, boolean readOnly,
		LocalDateTime trialStartedAt, LocalDateTime trialEndsAt, long daysRemaining,
		long staffUsed, int staffLimit, long petsUsed, int petsLimit,
		BigDecimal monthlyPrice, String currency, String billingCycle,
		Instant subscriptionStartedAt, Instant subscriptionEndsAt, long subscriptionDaysRemaining,
		Long lastPaymentId, boolean canActivate, boolean canRenew
) {}
