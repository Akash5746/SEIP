package com.seip.expense.mapper;

import com.seip.expense.dto.ExpenseDto;
import com.seip.expense.dto.ExpenseItemDto;
import com.seip.expense.dto.ExpenseSummaryDto;
import com.seip.expense.dto.ReceiptDto;
import com.seip.expense.entity.Expense;
import com.seip.expense.entity.ExpenseItem;
import com.seip.expense.entity.Receipt;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {ExpenseCategoryMapper.class})
public interface ExpenseMapper {

    ExpenseDto toDto(Expense expense);

    ExpenseSummaryDto toSummaryDto(Expense expense);

    ExpenseItemDto toItemDto(ExpenseItem item);

    List<ExpenseItemDto> toItemDtoList(List<ExpenseItem> items);

    ReceiptDto toReceiptDto(Receipt receipt);

    List<ReceiptDto> toReceiptDtoList(List<Receipt> receipts);
}
