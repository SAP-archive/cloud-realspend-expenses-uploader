package com.sap.expenseuploader;

import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.expenseuploader.config.ExpenseInputConfig;
import com.sap.expenseuploader.config.costcenter.CostCenterConfig;
import com.sap.expenseuploader.expenses.input.ExcelInput;
import com.sap.expenseuploader.expenses.output.ExcelOutput;
import com.sap.expenseuploader.model.Expense;
import org.junit.Test;

import java.io.File;
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
        ExpenseInputConfig eInConfig = mock(ExpenseInputConfig.class);
        when(eInConfig.getJcoDestination()).thenReturn(JCoDestinationManager.getDestination("SYSTEM"));
        when(eInConfig.getFromTime()).thenReturn("20120101");
        when(eInConfig.getToTime()).thenReturn("20170101");
        when(eInConfig.getControllingArea()).thenReturn("0001");
        when(eInConfig.getPeriod()).thenReturn("004");

        CostCenterConfig eOutConfig = mock(CostCenterConfig.class);
        when(eOutConfig.getCostCenterList()).thenReturn(Arrays.asList("MARKETING", "SAP-DUMMY"));

        // TODO use this on an ERP test system
    }

    @Test
    public void testExcelInput()
    {
        try {
            ExcelInput excelInput = new ExcelInput("src/test/resources/Input_Excel.xls");
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
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("2000-01-01", "FOO", "cc", "acc", "pers", "ord", "con", "req", "1090.01", "cur"));
        ExcelOutput output = new ExcelOutput("expenses.xls");
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
        sourceExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "1", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "1", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "7", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "5", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "1", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "1", "EUR"));
        sourceExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "2", "EUR"));

        List<Expense> targetExpenses = new ArrayList<>();
        targetExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "1", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "1", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "8", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "9", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "5", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "5", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "7", "EUR"));
        targetExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "1", "EUR"));

        List<Expense> expectedExpenses = new ArrayList<>();
        expectedExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "1", "EUR"));
        expectedExpenses.add(new Expense("2015-01-01", "ACTUAL", "", "", "", "", "", "", "2", "EUR"));

        assertEquals(expectedExpenses, Helper.getExpensesToAdd(sourceExpenses, targetExpenses));
    }
}
