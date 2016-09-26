package com.sap.expenseuploader;

import com.sap.expenseuploader.model.Expense;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents a library of helper functions which can be used by other classes
 */
public class Helper
{
    private static final Logger logger = LogManager.getLogger(Helper.class);

    // Deltamerge of expenses
    // Returns all sourceExpenses, that do not exist already in targetExpenses
    public static List<Expense> getExpensesToAdd( List<Expense> sourceExpenses, List<Expense> targetExpenses )
    {
        List<Expense> result = new ArrayList<>();
        int i = 0;
        int j = 0;

        // Both lists have to be sorted
        Collections.sort(sourceExpenses);
        Collections.sort(targetExpenses);

        while( true ) {
            if( sourceExpenses.size() <= i ) {
                // Done with all sourceExpenses
                return result;
            }
            Expense sourceExpense = sourceExpenses.get(i);
            if( targetExpenses.size() <= j ) {
                // Done with all targetExpenses
                result.add(sourceExpense);
            }
            Expense targetExpense = targetExpenses.get(j);
            if( sourceExpense.compareTo(targetExpense) == -1 ) {
                // This expense is smaller, add and skip
                result.add(sourceExpense);
                i++;
            } else if( sourceExpense.compareTo(targetExpense) == 1 ) {
                // This expense is larger, skip one target
                j++;
            } else {
                // Expenses are equal, skip both
                i++;
                j++;
            }
        }
    }

    public static String stripLeadingZeros( String str )
    {
        return str.replaceFirst("^0+(?!$)", "");
    }

    public static Object getCellValue( Cell cell )
    {
        switch( cell.getCellType() ) {
            case Cell.CELL_TYPE_STRING:
                return cell.getStringCellValue();
            case Cell.CELL_TYPE_BOOLEAN:
                return cell.getBooleanCellValue();
            case Cell.CELL_TYPE_NUMERIC:
                return cell.getNumericCellValue();
        }

        return null;
    }
}
