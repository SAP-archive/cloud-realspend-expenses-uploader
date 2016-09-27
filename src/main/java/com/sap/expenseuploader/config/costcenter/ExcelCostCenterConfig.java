package com.sap.expenseuploader.config.costcenter;

import com.sap.expenseuploader.Helper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

public class ExcelCostCenterConfig extends CostCenterConfig
{
    private final Logger logger = LogManager.getLogger(this.getClass());

    public ExcelCostCenterConfig(String path)
            throws InvalidFormatException, IOException
    {
        logger.debug("Reading cost centers for each user from Excel");

        File inputFile = new File(path);
        OPCPackage pkg = OPCPackage.open(inputFile);
        XSSFWorkbook workbook = new XSSFWorkbook(pkg);
        DataFormatter dataFormatter = new DataFormatter();

        // Get row iterator for expenses sheet
        XSSFSheet expensesSheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = expensesSheet.iterator();

        // Skip header
        if (rowIterator.hasNext()) {
            rowIterator.next();
        } else {
            logger.error("There should be rows here ...");
        }

        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();

            String user = dataFormatter.formatCellValue(cellIterator.next());
            String costCenter = dataFormatter.formatCellValue(cellIterator.next());

            if (!userCostCenters.containsKey(user)) {
                userCostCenters.put(user, new ArrayList<String>());
            }
            userCostCenters.get(user).add(Helper.stripLeadingZeros(costCenter));

            if (!costCenterUsers.containsKey(costCenter)) {
                costCenterUsers.put(costCenter, new ArrayList<String>());
            }
            costCenterUsers.get(costCenter).add(user);
        }
    }
}
