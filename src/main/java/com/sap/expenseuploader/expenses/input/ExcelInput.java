package com.sap.expenseuploader.expenses.input;

import com.sap.expenseuploader.model.Expense;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads expenses from an excel sheet. This enables either uploading external expenses (not
 * from the ERP system), or modifying the expenses exported as excel from the ERP.
 * Example excel sheet can be found in /test/resources
 *
 * The entire first sheet of the Excel file is used as input. Command line parameters are not respected.
 */
public class ExcelInput implements ExpenseInput
{
    private static final Logger logger = LogManager.getLogger(ExcelInput.class);

    private File inputFile;

    public ExcelInput( String path )
    {
        this.inputFile = new File(path);
    }

    @Override
    public List<Expense> getExpenses()
        throws IOException
    {
        List<Expense> expenses = new ArrayList<>();

        try( FileInputStream inputStream = new FileInputStream(this.inputFile) ) {
            Workbook workbook = new HSSFWorkbook(inputStream);
            Sheet firstSheet = workbook.getSheetAt(0);

            int cellsNumber = 0;
            for( Row nextRow : firstSheet ) {
                try {
                    if( nextRow.getRowNum() == 0 ) {
                        cellsNumber = nextRow.getPhysicalNumberOfCells();
                        // Skip header
                        continue;
                    }
                    List<String> rowFields = new ArrayList<>();
                    for( int cn = 0; cn < cellsNumber; cn++ ) {
                        try {
                            // TODO: Why blank?
                            final Cell cell = nextRow.getCell(cn, Row.RETURN_NULL_AND_BLANK);
                            if( cell == null ) {
                                rowFields.add(null);
                            } else {
                                String cellValue = getCellValue(cell);
                                rowFields.add(cellValue);
                            }
                        }
                        catch( Exception e ) {
                            throw new RuntimeException("Error in cell index " + cn, e);
                        }
                    }
                    Expense expense = new Expense(rowFields);
                    expenses.add(expense);
                }
                catch( Exception e ) {
                    logger.error("Error in row " + nextRow.getRowNum(), e);
                }
            }
        }
        return expenses;
    }

    private String getCellValue( Cell cell )
    {
        DataFormatter dataFormatter = new DataFormatter();
        return dataFormatter.formatCellValue(cell);
    }
}
