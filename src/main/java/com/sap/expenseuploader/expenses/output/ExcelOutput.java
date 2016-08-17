package com.sap.expenseuploader.expenses.output;

import com.sap.expenseuploader.Config;
import com.sap.expenseuploader.model.Expense;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

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
public class ExcelOutput extends AbstractOutput
{
    public ExcelOutput( Config config )
    {
        super(config);
    }

    @Override
    public boolean putExpenses( List<Expense> expenses )
    {

        int rowCount = 0;
        File file = new File(config.getOutput());
        try( final Workbook wb = new HSSFWorkbook() ) {
            FileOutputStream fileOut = new FileOutputStream(file);
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
            System.out.println("Error writing to file: " + file.getAbsolutePath());
            e.printStackTrace();
            return false;
        }
        System.out.println("Wrote " + rowCount + " expenses into XLS file " + file.getAbsolutePath());
        return true;
    }
}
