package com.sap.expenseuploader;

import com.sap.expenseuploader.config.budget.BudgetConfig;
import com.sap.expenseuploader.config.budget.ExcelBudgetConfig;
import com.sap.expenseuploader.model.BudgetEntry;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExcelBudgetConfigTest {

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testWorkingExcelConfig() throws IOException, InvalidFormatException
    {
        BudgetConfig config = new ExcelBudgetConfig("src/test/resources/config/working.xlsx");

        // Cost Center Budgets
        assertTrue(config.getMasterDataBudgetsOfUser("S12345670", "costcenter").get("1104341")
            .contains(new BudgetEntry(5000000, "EUR", 2015)));
        assertTrue(config.getMasterDataBudgetsOfUser("S12345678", "costcenter").get("1104342")
            .contains(new BudgetEntry(100000, "EUR", 2015)));
        assertTrue(config.getMasterDataBudgetsOfUser("S12345679", "costcenter").get("1104340")
            .contains(new BudgetEntry(4000000, "EUR", 2015)));
        assertEquals(1, config.getMasterDataBudgetsOfUser("S12345670", "costcenter").get("1104341").size());
        assertEquals(1, config.getMasterDataBudgetsOfUser("S12345678", "costcenter").get("1104342").size());
        assertEquals(1, config.getMasterDataBudgetsOfUser("S12345679", "costcenter").get("1104340").size());

        // Account Budgets
        assertTrue(config.getMasterDataBudgetsOfUser("S12345670", "account").get("6478000000")
                .contains(new BudgetEntry(300, "EUR", 2015)));
        assertTrue(config.getMasterDataBudgetsOfUser("S12345678", "account").get("6490512000")
                .contains(new BudgetEntry(100, "EUR", 2015)));
        assertTrue(config.getMasterDataBudgetsOfUser("S12345679", "account").get("6414000000")
                .contains(new BudgetEntry(200, "EUR", 2015)));
        assertEquals(1, config.getMasterDataBudgetsOfUser("S12345670", "account").get("6478000000").size());
        assertEquals(1, config.getMasterDataBudgetsOfUser("S12345678", "account").get("6490512000").size());
        assertEquals(1, config.getMasterDataBudgetsOfUser("S12345679", "account").get("6414000000").size());

        // Tag Budgets
        assertTrue(config.getTagBudgetsOfUser("S12345670").get("Products").get("Product A")
            .contains(new BudgetEntry(1000, "EUR", 2015)));
        assertEquals(1, config.getTagBudgetsOfUser("S12345670").size());
        assertEquals(3, config.getTagBudgetsOfUser("S12345670").get("Products").size());
        assertEquals(1, config.getTagBudgetsOfUser("S12345670").get("Products").get("Product A").size());
    }

    @Test
    public void testDoubleBudgetExcelConfig() throws IOException, InvalidFormatException {
        exception.expect(InvalidFormatException.class);
        new ExcelBudgetConfig("src/test/resources/config/double_budget.xlsx");
    }

    @Test
    public void testNotANumberExcelConfig() throws IOException, InvalidFormatException {
        exception.expect(InvalidFormatException.class);
        new ExcelBudgetConfig("src/test/resources/config/not_a_number.xlsx");
    }
}
