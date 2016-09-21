package com.sap.expenseuploader;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class BudgetConfigTest {

    // Prototype for the new excel format as budget configuration

    @Test
    public void testCurrencyRead() throws IOException, InvalidFormatException {
        File inputFile = new File("src/test/resources/test_budget_conf.xlsx");
        OPCPackage pkg = OPCPackage.open(inputFile);

        XSSFWorkbook workbook = new XSSFWorkbook(pkg);
        XSSFSheet accountSheet = workbook.getSheetAt(1);
        Iterator<Row> rowIterator = accountSheet.iterator();

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();
            while (cellIterator.hasNext()) {
                Cell cell = cellIterator.next();
                if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
                    DataFormatter formatter = new DataFormatter();
                    String formattedCellValue = formatter.formatCellValue(cell);

                    System.out.println(String.format("[%s, %s] %s %s",
                        cell.getRowIndex(),
                        cell.getColumnIndex(),
                        formattedCellValue.replaceAll("[^0-9.]", ""),
                        formattedCellValue.replaceAll("[0-9 .]", "")
                    ));
                }
                else {
                    System.out.println(String.format("[%s, %s] %s", cell.getRowIndex(), cell.getColumnIndex(), cell.getStringCellValue()));
                }
            }
        }
    }
}
