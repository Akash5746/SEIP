package com.seip.fraud.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

/**
 * Reactive client for the expense-service.
 * Fetches duplicate-claim status and monthly claim count for a given employee.
 */
@Slf4j
@Service
public class ExpenseServiceClient {

    private final WebClient expenseServiceWebClient;

    public ExpenseServiceClient(@Qualifier("expenseServiceWebClient") WebClient expenseServiceWebClient) {
        this.expenseServiceWebClient = expenseServiceWebClient;
    }

    /**
     * Check whether a similar expense already exists for this employee.
     *
     * @param employeeId   the employee making the claim
     * @param amount       expense amount
     * @param merchantName merchant name
     * @return Mono<Boolean> — true if a duplicate is found, false otherwise (including on error)
     */
    public Mono<Boolean> checkDuplicate(Long employeeId, BigDecimal amount, String merchantName) {
        return expenseServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/expenses/check-duplicate")
                        .queryParam("employeeId", employeeId)
                        .queryParam("amount", amount)
                        .queryParam("merchantName", merchantName)
                        .queryParam("withinDays", 30)
                        .build())
                .retrieve()
                .bodyToMono(Boolean.class)
                .doOnSuccess(dup -> log.debug("Duplicate check for employeeId={}: {}", employeeId, dup))
                .onErrorResume(ex -> {
                    log.warn("Failed to check duplicate from expense-service for employeeId={}: {}",
                            employeeId, ex.getMessage());
                    return Mono.just(false);
                });
    }

    /**
     * Retrieve the number of expenses submitted by the employee in the current month.
     *
     * @param employeeId the target employee
     * @return Mono<Integer> — count, or 0 on error
     */
    public Mono<Integer> getMonthlyClaimCount(Long employeeId) {
        return expenseServiceWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/expenses/monthly-count")
                        .queryParam("employeeId", employeeId)
                        .build())
                .retrieve()
                .bodyToMono(Integer.class)
                .doOnSuccess(count -> log.debug("Monthly claim count for employeeId={}: {}", employeeId, count))
                .onErrorResume(ex -> {
                    log.warn("Failed to fetch monthly claim count from expense-service for employeeId={}: {}",
                            employeeId, ex.getMessage());
                    return Mono.just(0);
                });
    }
}
