package com.seip.expense.repository;

import com.seip.expense.entity.Expense;
import com.seip.expense.entity.ExpenseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    Optional<Expense> findByExpenseNumber(String expenseNumber);

    Page<Expense> findByEmployeeId(Long employeeId, Pageable pageable);

    Page<Expense> findByEmployeeIdIn(List<Long> employeeIds, Pageable pageable);

    Page<Expense> findByEmployeeIdInAndStatusIn(List<Long> employeeIds, List<ExpenseStatus> statuses, Pageable pageable);

    Page<Expense> findByStatusIn(List<ExpenseStatus> statuses, Pageable pageable);

    @Query("SELECT MAX(e.id) FROM Expense e")
    Optional<Long> findMaxId();

    @Query("""
            SELECT COUNT(e) > 0 FROM Expense e
            WHERE e.employeeId = :employeeId
              AND e.amount = :amount
              AND e.merchantName = :merchantName
              AND e.expenseDate >= :fromDate
              AND e.status NOT IN ('REJECTED', 'DRAFT')
            """)
    boolean existsDuplicateExpense(@Param("employeeId") Long employeeId,
                                   @Param("amount") BigDecimal amount,
                                   @Param("merchantName") String merchantName,
                                   @Param("fromDate") LocalDate fromDate);

    @Query("""
            SELECT COUNT(e) FROM Expense e
            WHERE e.employeeId = :employeeId
              AND e.submittedAt >= :from
              AND e.submittedAt < :to
            """)
    int countMonthlySubmissions(@Param("employeeId") Long employeeId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}
