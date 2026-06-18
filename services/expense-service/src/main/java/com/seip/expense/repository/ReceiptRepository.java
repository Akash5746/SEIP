package com.seip.expense.repository;

import com.seip.expense.entity.Receipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReceiptRepository extends JpaRepository<Receipt, Long> {

    List<Receipt> findByExpenseId(Long expenseId);

    Optional<Receipt> findByIdAndExpenseEmployeeId(Long id, Long employeeId);
}
