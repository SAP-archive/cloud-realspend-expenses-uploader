package com.sap.expenseuploader.expenses.output;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sap.expenseuploader.model.Expense;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Prints out expenses to the system console. This class can be a facilitator
 * for debugging and checking the expenses read from the ERP.
 */
public class CliOutput implements ExpenseOutput
{
    private final Logger logger = LogManager.getLogger(this.getClass());

    public CliOutput() {}

    @Override
    public void putExpenses( List<Expense> expenses )
    {
        logger.info("Writing expenses to command line");

        for( Expense expense : expenses ) {
            Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
            System.out.println(gson.toJson(expense));
        }

        logger.info("Done writing expenses to command line");
    }
}
