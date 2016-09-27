package com.sap.expenseuploader.expenses.output;

import com.sap.expenseuploader.model.Expense;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Outputs the expenses to an excel sheet, with the same format of the file in
 * /test/resources
 * This can be used as a middle step between reading expenses from the ERP
 * and uploading them to the HCP backend. This step enables changes and checks to those
 * expenses from the ERP
 */
public class ExcelOutput implements ExpenseOutput
{
    private final Logger logger = LogManager.getLogger(this.getClass());

    private File outputFile;

    public ExcelOutput( String path )
    {
        this.outputFile = new File(path);
    }

    @Override
    public void putExpenses( List<Expense> expenses )
    {
        logger.info("Writing expenses to excel file " + this.outputFile.getAbsolutePath());

        int rowCount = 0;
        try (final Workbook wb = new SXSSFWorkbook(new XSSFWorkbook())) {
            FileOutputStream fileOut = new FileOutputStream(this.outputFile);
            Sheet sheet = wb.createSheet("Sheet");

            // Write first line
            List<String> headers = Arrays.asList("Item Date",
                "Cost Type",
                "Cost Center",
                "Account",
                "Requester Person",
                "Internal Order",
                "Context",
                "Request ID",
                "Amount",
                "Currency");
            Row row = sheet.createRow(0);
            for( int j = 0; j < headers.size(); ++j ) {
                Cell cell = row.createCell(j);
                cell.setCellValue(headers.get(j));
            }

            // Write all expenses
            for( int i = 0; i < expenses.size(); ++i ) {
                Expense rowContent = expenses.get(i);
                row = sheet.createRow(i + 1); // Off by one because of header row
                for( int j = 0; j < rowContent.size(); ++j ) {
                    Cell cell = row.createCell(j);
                    cell.setCellValue(rowContent.get(j));
                }
                rowCount += 1;
            }

            // Close
            wb.write(fileOut);
            fileOut.flush();
            fileOut.close();
        }
        catch( IOException e ) {
            logger.error("Error writing to file: " + this.outputFile.getAbsolutePath());
            e.printStackTrace();
        }
        logger.info("Wrote " + rowCount + " expenses into XLS file " + this.outputFile.getAbsolutePath());
    }
}
