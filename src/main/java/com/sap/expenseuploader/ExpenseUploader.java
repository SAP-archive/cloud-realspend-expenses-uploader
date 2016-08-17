package com.sap.expenseuploader;

import com.sap.expenseuploader.budgets.BudgetHcpOutput;
import com.sap.expenseuploader.expenses.input.AbstractInput;
import com.sap.expenseuploader.expenses.input.ErpInput;
import com.sap.expenseuploader.expenses.input.ExcelInput;
import com.sap.expenseuploader.expenses.output.AbstractOutput;
import com.sap.expenseuploader.expenses.output.CliOutput;
import com.sap.expenseuploader.expenses.output.ExcelOutput;
import com.sap.expenseuploader.expenses.output.ExpenseHcpOutput;
import com.sap.expenseuploader.model.Expense;
import org.apache.commons.cli.*;

import java.util.List;

/**
 * represents the main entry point for the program
 */
public class ExpenseUploader
{
    public static void main( String[] args )
    {
        try {
            System.out.println("--- Expense & Budget Uploader ---");

            Options options = new Options();
            options.addOption("f", "from", true, "lower posting date in YYYYMMDD format");
            options.addOption("t", "to", true, "higher posting date in YYYYMMDD format (optional)");
            options.addOption("ca", "controlling-area", true, "controlling area, e.g. 0001");
            options.addOption("in_erp", "input_erp", true, "id of input system");
            options.addOption("in_xls", "input_xls", true, "path to input excel file");
            options.addOption("out_cli", "output_cli", false, "write to command line");
            options.addOption("out_hcp",
                "output_hcp",
                true,
                "output url, e.g. https://bmsfin-<accountId>.hanatrial.ondemand.com/core/api/v1");
            options.addOption("out_xls", "output_xls", true, "path to output excel file");
            options.addOption("p", "period", true, "period (optional), e.g. 001");
            options.addOption("with_budgets", "with_budgets", false, "with budgets (optional)");
            options.addOption("u", "hcp_user", true, "your hcp username (optional), e.g. hans");
            options.addOption("ps", "hcp_password", true, "your hcp password (optional)");
            options.addOption("x", "proxy", true, "proxy server (optional), e.g. example.com:8080");
            options.addOption("h", "help", false, "print this message");
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            if( cmd.hasOption("h") || !cmd.hasOption("f") || !cmd.hasOption("ca") ) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(" ", options);
                return;
            }

            // Init config
            Config config = new Config(cmd.getOptionValue("ca"),
                cmd.getOptionValue("f"),
                cmd.getOptionValue("t"),
                cmd.getOptionValue("p"),
                cmd.getOptionValue("u"),
                cmd.getOptionValue("ps"),
                cmd.getOptionValue("x"),
                "budgets.json",
                "costcenters.json",
                cmd.hasOption("with_budgets"));

            // Get input and output
            int inputCount = 0;
            int outputCount = 0;
            AbstractInput input = null;
            AbstractOutput output = null;
            if( cmd.hasOption("in_erp") ) {
                config.setInput(cmd.getOptionValue("in_erp"));
                input = new ErpInput(config);
                inputCount++;
            }
            if( cmd.hasOption("in_xls") ) {
                config.setInput(cmd.getOptionValue("in_xls"));
                input = new ExcelInput(config);
                inputCount++;
            }
            if( cmd.hasOption("out_cli") ) {
                output = new CliOutput(config);
                outputCount++;
            }
            if( cmd.hasOption("out_hcp") ) {
                System.out.println("Setting output to " + cmd.getOptionValue("out_hcp"));
                config.setOutput(cmd.getOptionValue("out_hcp"));
                output = new ExpenseHcpOutput(config);
                outputCount++;
            }
            if( cmd.hasOption("out_xls") ) {
                config.setOutput(cmd.getOptionValue("out_xls"));
                output = new ExcelOutput(config);
                outputCount++;
            }
            if( inputCount != 1 || outputCount != 1 ) {
                System.out.println("Please specify exactly one input and one output!");
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(" ", options);
                return;
            }

            // Do the work
            // 1- uploading expenses
            List<Expense> expenses = input.getExpenses();
            if( expenses == null || expenses.size() == 0 ) {
                System.out.println("No expenses found!");
                return;
            }
            boolean success = output.putExpenses(expenses);
            if( success ) {
                System.out.println("Uploading Expenses Success!");
            } else {
                System.out.println("Uploading Expenses has errors!");
            }

            // 2- uploading budgets (if required)
            if( cmd.hasOption("with_budgets") ) {
                BudgetHcpOutput budgetHcpOutput = new BudgetHcpOutput(config);
                budgetHcpOutput.putBudgets(Helper.getCsrfToken(config));
            }
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }
}
