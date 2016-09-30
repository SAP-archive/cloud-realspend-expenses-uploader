package com.sap.expenseuploader.config.budget;

import com.sap.expenseuploader.model.BudgetEntry;
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
import java.util.*;

public class ExcelBudgetConfig extends BudgetConfig
{
    private final Logger logger = LogManager.getLogger(this.getClass());
    private final DataFormatter dataFormatter = new DataFormatter();

    public ExcelBudgetConfig( String path )
        throws IOException, InvalidFormatException
    {
        logger.debug("Reading budgets for each user from Excel");

        File inputFile = new File(path);
        OPCPackage pkg = OPCPackage.open(inputFile);
        XSSFWorkbook workbook = new XSSFWorkbook(pkg);

        // Read budgets
        readOverallBudgets(workbook.getSheetAt(1));
        readMasterDataBudgets(workbook.getSheetAt(2), "costcenter");
        readMasterDataBudgets(workbook.getSheetAt(3), "account");
        readTagBudgets(workbook.getSheetAt(4));
    }

    private void readOverallBudgets( XSSFSheet sheet )
        throws InvalidFormatException
    {
        Iterator<Row> rowIterator = sheet.iterator();

        // Skip header
        if( rowIterator.hasNext() ) {
            rowIterator.next();
        } else {
            logger.error("There should be a header row here ...");
        }

        while( rowIterator.hasNext() ) {
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();

            Cell cell = cellIterator.next();
            String year = dataFormatter.formatCellValue(cell);
            if( year.isEmpty() ) {
                logger.debug("Skipping empty line ...");
                continue;
            }
            cell = cellIterator.next();
            String user = dataFormatter.formatCellValue(cell);
            cell = cellIterator.next();
            double budget = getBudgetFromCell(cell, "overall");
            cell = cellIterator.next();
            String currency = dataFormatter.formatCellValue(cell);

            if( !userOverallBudgets.containsKey(user) ) {
                userOverallBudgets.put(user, new ArrayList<BudgetEntry>());
            }
            if( hasExistingBudget(userOverallBudgets.get(user), year) ) {
                String errorMessage =
                    String.format("Duplicate overall budget detected for user %s and year %s", user, year);
                logger.error(errorMessage);
                throw new InvalidFormatException(errorMessage);
            }
            BudgetEntry entry = new BudgetEntry(budget, currency, Long.parseLong(year));
            userOverallBudgets.get(user).add(entry);
        }
    }

    private void readMasterDataBudgets( XSSFSheet sheet, String masterDataType )
        throws InvalidFormatException
    {
        Iterator<Row> rowIterator = sheet.iterator();

        // Skip header
        if( rowIterator.hasNext() ) {
            rowIterator.next();
        } else {
            logger.error("There should be a header row here ...");
        }

        while( rowIterator.hasNext() ) {
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();

            Cell cell = cellIterator.next();
            String year = dataFormatter.formatCellValue(cell);
            if( year.isEmpty() ) {
                logger.debug("Skipping empty line ...");
                continue;
            }
            cell = cellIterator.next();
            String user = dataFormatter.formatCellValue(cell);
            cell = cellIterator.next();
            String masterDataName = dataFormatter.formatCellValue(cell);
            cell = cellIterator.next();
            double budget = getBudgetFromCell(cell, masterDataType);
            cell = cellIterator.next();
            String currency = dataFormatter.formatCellValue(cell);

            if( !userMasterDataBudgets.containsKey(user) ) {
                userMasterDataBudgets.put(user, new HashMap<String, Map<String, List<BudgetEntry>>>());
            }
            if( !userMasterDataBudgets.get(user).containsKey(masterDataType) ) {
                userMasterDataBudgets.get(user).put(masterDataType, new HashMap<String, List<BudgetEntry>>());
            }
            if( !userMasterDataBudgets.get(user).get(masterDataType).containsKey(masterDataName) ) {
                userMasterDataBudgets.get(user).get(masterDataType).put(masterDataName, new ArrayList<BudgetEntry>());
            }
            if( hasExistingBudget(userMasterDataBudgets.get(user).get(masterDataType).get(masterDataName), year) ) {
                String errorMessage = String.format("Duplicate budget detected for user %s, %s '%s' and year %s",
                    user,
                    masterDataType,
                    masterDataName,
                    year);
                logger.error(errorMessage);
                throw new InvalidFormatException(errorMessage);
            }
            BudgetEntry entry = new BudgetEntry(budget, currency, Long.parseLong(year));
            userMasterDataBudgets.get(user).get(masterDataType).get(masterDataName).add(entry);
        }
    }

    private void readTagBudgets( XSSFSheet sheet )
        throws InvalidFormatException
    {
        Iterator<Row> rowIterator = sheet.iterator();

        // Skip header
        if( rowIterator.hasNext() ) {
            rowIterator.next();
        } else {
            logger.error("There should be a header row here ...");
        }

        while( rowIterator.hasNext() ) {
            Row row = rowIterator.next();
            Iterator<Cell> cellIterator = row.cellIterator();

            Cell cell = cellIterator.next();
            String year = dataFormatter.formatCellValue(cell);
            if( year.isEmpty() ) {
                logger.debug("Skipping empty line ...");
                continue;
            }
            cell = cellIterator.next();
            String user = dataFormatter.formatCellValue(cell);
            cell = cellIterator.next();
            String tagGroup = dataFormatter.formatCellValue(cell);
            cell = cellIterator.next();
            String tagName = dataFormatter.formatCellValue(cell);
            cell = cellIterator.next();
            double budget = getBudgetFromCell(cell, "tag");
            cell = cellIterator.next();
            String currency = dataFormatter.formatCellValue(cell);

            if( !userTagBudgets.containsKey(user) ) {
                userTagBudgets.put(user, new HashMap<String, Map<String, List<BudgetEntry>>>());
            }
            if( !userTagBudgets.get(user).containsKey(tagGroup) ) {
                userTagBudgets.get(user).put(tagGroup, new HashMap<String, List<BudgetEntry>>());
            }
            if( !userTagBudgets.get(user).get(tagGroup).containsKey(tagName) ) {
                userTagBudgets.get(user).get(tagGroup).put(tagName, new ArrayList<BudgetEntry>());
            }
            if( hasExistingBudget(userTagBudgets.get(user).get(tagGroup).get(tagName), year) ) {
                String errorMessage = String.format(
                    "Duplicate budget detected for user %s, tag group %s, tag %s and year %s",
                    user,
                    tagGroup,
                    tagName,
                    year);
                logger.error(errorMessage);
                throw new InvalidFormatException(errorMessage);
            }
            BudgetEntry entry = new BudgetEntry(budget, currency, Long.parseLong(year));
            userTagBudgets.get(user).get(tagGroup).get(tagName).add(entry);
        }
    }

    private double getBudgetFromCell( Cell cell, String type )
        throws InvalidFormatException
    {
        if( cell.getCellType() != Cell.CELL_TYPE_NUMERIC ) {
            String errorMessage = String.format("Budget value '%s' (in %s budgets) is not a valid number!",
                dataFormatter.formatCellValue(cell),
                type);
            logger.error(errorMessage);
            throw new InvalidFormatException(errorMessage);
        }
        return cell.getNumericCellValue();
    }

    private boolean hasExistingBudget( List<BudgetEntry> entries, String year )
    {
        for( BudgetEntry entry : entries ) {
            if( entry.year == Long.parseLong(year) ) {
                return true;
            }
        }
        return false;
    }
}
