package com.sap.expenseuploader.expenses.output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sap.expenseuploader.Config;
import com.sap.expenseuploader.model.Expense;

import java.util.List;

/**
 * Prints out the inputted expenses to the system console. This class
 * can be a facilitator for debugging and checking the expenses read from the ERP.
 */
public class CliOutput extends AbstractOutput
{
    public CliOutput( Config config )
    {
        super(config);
    }

    @Override
    public boolean putExpenses( List<Expense> expenses )
    {

        for( Expense expense : expenses ) {
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
            System.out.println(gson.toJson(expense));
        }

        return true;
    }
}
