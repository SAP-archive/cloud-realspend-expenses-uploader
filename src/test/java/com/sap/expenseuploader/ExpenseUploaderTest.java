package com.sap.expenseuploader;

import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.expenseuploader.expenses.input.ExcelInput;
import com.sap.expenseuploader.expenses.output.ExcelOutput;
import com.sap.expenseuploader.model.Expense;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExpenseUploaderTest
{
    @Test
    public void testErpInput()
        throws JCoException
    {
        Config config = mock(Config.class);
        when(config.getJcoDestination("SYSTEM")).thenReturn(JCoDestinationManager.getDestination("SYSTEM"));
        when(config.getFromTime()).thenReturn("20120101");
        when(config.getToTime()).thenReturn("20170101");
        when(config.getControllingArea()).thenReturn("0001");
        when(config.getPeriod()).thenReturn("004");
        when(config.getCostCenterList()).thenReturn(Arrays.asList("MARKETING", "SAP-DUMMY"));
    }

    @Test
    public void testExcelInput()
    {
        try {
            Config config = mock(Config.class);
            when(config.getInput()).thenReturn("src/test/resources/Input_Excel.xls");
            ExcelInput excelInput = new ExcelInput(config);
            List<Expense> expenses = excelInput.getExpenses();
            assertEquals(54, expenses.size());
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    @Test
    public void testExcelOutput()
        throws ParseException
    {
        Config config = mock(Config.class);
        when(config.getOutput()).thenReturn("expenses.xls");
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("2000-01-01", "FOO", "cc", "acc", "pers", "ord", "con", "req", "1090.01", "cur"));
        ExcelOutput output = new ExcelOutput(config);
        output.putExpenses(expenses);
        File outputFile = new File("expenses.xls");
        assertTrue(outputFile.exists());
        assertNotEquals(0, outputFile.length());
        outputFile.delete();
    }

    @Test
    public void testDeltamerge()
        throws ParseException
    {
        List<Expense> sourceExpenses = new ArrayList<>();
        sourceExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "1", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "1", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "7", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "5", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "1", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "1", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "2", "EUR"));

        List<Expense> targetExpenses = new ArrayList<>();
        targetExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "1", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "1", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "8", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "9", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "5", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "5", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "7", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "1", "EUR"));

        List<Expense> expectedExpenses = new ArrayList<>();
        expectedExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "1", "EUR"));
        expectedExpenses.add(new Expense("2015-01-01", null, null, null, null, null, null, null, "2", "EUR"));

        assertEquals(expectedExpenses, Helper.getExpensesToAdd(sourceExpenses, targetExpenses));
    }

    @Test
    public void testConfig()
        throws IOException, ParseException, org.json.simple.parser.ParseException
    {
        Config config = new Config("",
            "",
            "",
            "",
            "",
            "",
            "",
            "src/test/resources/budgets.json",
            "src/test/resources/costcenters.json",
            false);
        assertEquals("[0001, 0002, 0019, testy]", config.getCostCenterList().toString());
        assertEquals("[alex, bob, d061519]", config.getUserList().toString());
    }

    @Test
    public void testTagBudgets()
        throws IOException, org.json.simple.parser.ParseException
    {
        Config config = new Config("",
            "",
            "",
            "",
            "",
            "",
            "",
            "src/test/resources/budgets.json",
            "src/test/resources/costcenters.json",
            false);
        assertEquals("{}", config.getTagBudgetsOfUser("alex").toString());
    }

    // TODO Integration tests
    // TODO Load from ERP, save to XLS
    // TODO Load from XLS, save to HCP
    // TODO Load from ERP, save to HCP
    // TODO Load from XLS, save to XLS (there should be an error)

}
