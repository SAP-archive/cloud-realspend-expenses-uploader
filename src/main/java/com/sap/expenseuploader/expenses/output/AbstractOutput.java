package com.sap.expenseuploader.expenses.output;

import com.sap.expenseuploader.Config;
import com.sap.expenseuploader.model.Expense;

import java.util.List;

/**
 * Each subclass of this parent abstract class represents an output destination for
 * expenses
 */
public abstract class AbstractOutput
{
    protected Config config;

    public AbstractOutput( Config config )
    {
        this.config = config;
    }

    public abstract boolean putExpenses( List<Expense> expenses );
}
