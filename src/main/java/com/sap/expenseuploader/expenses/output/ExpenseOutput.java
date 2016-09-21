package com.sap.expenseuploader.expenses.output;

import com.sap.expenseuploader.model.Expense;

import java.util.List;

public interface ExpenseOutput
{
    void putExpenses( List<Expense> expenses );
}
