package com.seip.expense.util;

import com.seip.expense.repository.ExpenseRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpenseNumberGenerator {

    private final ExpenseRepository expenseRepository;

    private AtomicLong counter;

    @PostConstruct
    public void init() {
        Long maxId = expenseRepository.findMaxId().orElse(0L);
        // Seed the counter from DB max ID; add a timestamp-based offset to ensure uniqueness on restarts
        long seed = Math.max(maxId, System.currentTimeMillis() % 1_000_000L);
        counter = new AtomicLong(seed);
        log.info("ExpenseNumberGenerator initialized with counter seed: {}", seed);
    }

    /**
     * Generates expense number in format: EXP-{YYYY}-{6-digit-sequence}
     * Example: EXP-2026-000042
     */
    public String generateExpenseNumber() {
        int year = LocalDate.now().getYear();
        long sequence = counter.incrementAndGet();
        return String.format("EXP-%d-%06d", year, sequence % 1_000_000L);
    }
}
