package com.sap.expenseuploader.expenses.input;

import com.sap.expenseuploader.Config;
import com.sap.expenseuploader.model.Expense;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Reads expenses from an excel sheet. This enables either uploading external expenses (not
 * from the ERP system), or modifying the expenses exported as excel from the ERP.
 * Example excel sheet can be found in /test/resources
 */
public class ExcelInput extends AbstractInput
{
    private List<String> ALLOWED_EXPENSE_TYPES = Arrays.asList("ACTUAL");

    public ExcelInput( Config config )
    {
        super(config);
    }

    @Override
    public List<Expense> getExpenses()
        throws IOException, ParseException
    {
        FileInputStream inputStream = new FileInputStream(new File(config.getInput()));

        List<Expense> expenses = new ArrayList<>();

        Workbook workbook = new HSSFWorkbook(inputStream);
        Sheet firstSheet = workbook.getSheetAt(0);

        for( Row nextRow : firstSheet ) {
            if( nextRow.getRowNum() == 0 ) {
                // Skip header
                continue;
            }
            List<String> rowFields = new ArrayList<>();
            for( Cell nextCell : nextRow ) {
                rowFields.add((String) getCellValue(nextCell));
            }
            Expense expense = new Expense(rowFields);
            if( !ALLOWED_EXPENSE_TYPES.contains(expense.getType().toUpperCase()) ) {
                throw new ParseException("Unable to parse expense type: " + expense.getType(), -1);
            }
            expenses.add(expense);
        }

        workbook.close();
        inputStream.close();

        return expenses;
    }

    private Object getCellValue( Cell cell )
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
