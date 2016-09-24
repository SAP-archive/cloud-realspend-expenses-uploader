package com.sap.expenseuploader.expenses.input;

import com.sap.expenseuploader.model.Expense;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

public interface ExpenseInput
{
    List<Expense> getExpenses()
        throws IOException, ParseException;
}
