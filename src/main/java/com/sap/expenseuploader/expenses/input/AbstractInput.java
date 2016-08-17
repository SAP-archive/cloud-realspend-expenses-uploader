package com.sap.expenseuploader.expenses.input;

import com.sap.expenseuploader.Config;
import com.sap.expenseuploader.model.Expense;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

/**
 * Each subclass of this abstract class represents a source of input for expenses
 */
public abstract class AbstractInput
{
    protected Config config;

    public AbstractInput( Config config )
    {
        this.config = config;
    }

    public abstract List<Expense> getExpenses()
        throws IOException, ParseException;
}
