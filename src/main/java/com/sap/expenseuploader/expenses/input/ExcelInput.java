package com.sap.expenseuploader.expenses.input;

import com.sap.expenseuploader.model.Expense;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads expenses from an excel sheet. This enables either uploading external expenses (not
 * from the ERP system), or modifying the expenses exported as excel from the ERP.
 * Example excel sheet can be found in /test/resources
 * <p>
 * The entire first sheet of the Excel file is used as input. Command line parameters are not respected.
 */
public class ExcelInput implements ExpenseInput
{
    private File inputFile;

    public ExcelInput( String path )
    {
        this.inputFile = new File(path);
    }

    @Override
    public List<Expense> getExpenses()
        throws IOException
    {
        FileInputStream inputStream = new FileInputStream(this.inputFile);

        List<Expense> expenses = new ArrayList<>();

        Workbook workbook = new HSSFWorkbook(inputStream);
        Sheet firstSheet = workbook.getSheetAt(0);

        for( Row nextRow : firstSheet ) {
            if( nextRow.getRowNum() == 0 ) {
                // Skip header
                continue;
            }
            List<String> rowFields = new ArrayList<>();
            for( int cn = 0; cn < nextRow.getLastCellNum(); cn++ ) {
                Object cellValue = getCellValue(nextRow.getCell(cn, Row.CREATE_NULL_AS_BLANK));
                if( cellValue == null )
                    rowFields.add("");
                else
                    rowFields.add(String.valueOf(cellValue));
            }
            Expense expense = new Expense(rowFields);
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
