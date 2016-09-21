package com.sap.expenseuploader;

import com.sap.expenseuploader.budgets.BudgetHcpOutput;
import com.sap.expenseuploader.config.BudgetConfig;
import com.sap.expenseuploader.config.ExpenseInputConfig;
import com.sap.expenseuploader.config.CostcenterConfig;
import com.sap.expenseuploader.config.HcpConfig;
import com.sap.expenseuploader.expenses.input.ExpenseInput;
import com.sap.expenseuploader.expenses.input.ErpInput;
import com.sap.expenseuploader.expenses.input.ExcelInput;
import com.sap.expenseuploader.expenses.output.ExpenseOutput;
import com.sap.expenseuploader.expenses.output.CliOutput;
import com.sap.expenseuploader.expenses.output.ExcelOutput;
import com.sap.expenseuploader.expenses.output.ExpenseHcpOutput;
import com.sap.expenseuploader.model.Expense;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.relation.RoleNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the main entry point for the program
 */
public class ExpenseUploader
{
    private static final Logger logger = LogManager.getLogger(ExpenseUploader.class);

    public static void main( String[] args ) throws IOException, org.json.simple.parser.ParseException,
            ParseException, java.text.ParseException, RoleNotFoundException, URISyntaxException {
        logger.info("--- Expense & Budget Uploader ---");

        Options options = new Options();

        // General options
        options.addOption("url", "hcp_url", true,
                "account-specific URL of HCP, e.g. https://bmsfin-<accountId>.hanatrial.ondemand.com/core/api/v1");
        options.addOption("user", "hcp_user", true, "your hcp username (optional, will prompt), e.g. p12345trial");
        options.addOption("pass", "hcp_password", true, "your hcp password (optional, will prompt)");
        options.addOption("x", "hcp_proxy", true, "proxy server (optional), e.g. example.com:8080");
        options.addOption("h", "help", false, "print this message");
        options.addOption("b", "budgets", false, "upload budgets to HCP (optional, needs HCP URL)");

        // Options for Expenses
        options.addOption("in_erp", "input_erp", true,
            "id of the erp system used for expense input, default is 'system'");
        options.addOption("in_xls", "input_xls", true, "path to expense input excel file");
        options.addOption("out_cli", "output_cli", false, "write expenses to command line");
        options.addOption("out_hcp", "output_hcp", false, "write expenses to HCP");
        options.addOption("out_xls", "output_xls", true, "write expenses to excel file (path needed)");
        options.addOption("c", "controlling-area", true, "controlling area, e.g. 0001");
        options.addOption("f", "from", true, "lower posting date in YYYYMMDD format");
        options.addOption("t", "to", true, "higher posting date in YYYYMMDD format (optional)");
        options.addOption("p", "period", true, "period (optional), e.g. 001");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("h")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(" ", options);
            return;
        }

        // Prepare config for expenses, create inputs and outputs
        int expenseInputCounter = 0;
        int expenseOutputCounter = 0;
        ExpenseInput expenseInput = null;
        List<ExpenseOutput> expenseOutputs = new ArrayList<>();
        if (cmd.hasOption("in_erp")) {
            expenseInput = new ErpInput(
                new ExpenseInputConfig(
                    cmd.getOptionValue("in_erp"),
                    cmd.getOptionValue("c"),
                    cmd.getOptionValue("f"),
                    cmd.getOptionValue("t"),
                    cmd.getOptionValue("p")
                ),
                new CostcenterConfig()
            );
            expenseInputCounter++;
        }
        if (cmd.hasOption("in_xls")) {
            if (0 < expenseInputCounter) {
                logger.error("More than one input defined!");
                System.exit(1);
            }
            expenseInput = new ExcelInput(cmd.getOptionValue("in_xls"));
            expenseInputCounter++;
        }
        if (cmd.hasOption("out_cli")) {
            expenseOutputs.add(new CliOutput());
            expenseOutputCounter++;
        }
        if (cmd.hasOption("out_hcp")) {
            if ( !cmd.hasOption("url") ) {
                logger.error("Please specify the HCP URL to upload expenses");
                System.exit(1);
            }
            expenseOutputs.add(
                new ExpenseHcpOutput(
                    new HcpConfig(
                        cmd.getOptionValue("url"),
                        cmd.getOptionValue("user"),
                        cmd.getOptionValue("pass"),
                        cmd.getOptionValue("x")
                    ),
                    new CostcenterConfig()
                )
            );
            expenseOutputCounter++;
        }
        if (cmd.hasOption("out_xls")) {
            expenseOutputs.add(new ExcelOutput(cmd.getOptionValue("out_xls")));
            expenseOutputCounter++;
        }
        if (expenseInputCounter == 0 && expenseOutputCounter == 0) { // No expenses
            logger.info("No inputs or outputs defined, skipping expenses");
        }
        else if (expenseInputCounter == 1 && expenseOutputCounter != 0) { // Correct number of in- and outputs
            if ( !cmd.hasOption("f") || !cmd.hasOption("c") ) {
                logger.error("Please specify expense parameters 'from' and 'controlling-area'");
                System.exit(1);
            }
        }
        else {
            logger.error("Please specify for expenses either no inputs and no outputs" +
                " or exactly one input and one or more outputs.");
            System.exit(1);
        }

        // Prepare budgets
        if (cmd.hasOption("b") && !cmd.hasOption("url")) {
            logger.error("Please specify the HCP URL to upload budgets");
            System.exit(1);
        }

        // Do the work
        // 1: Upload expenses
        if (0 < expenseInputCounter) {
            logger.info("");
            List<Expense> expenses = expenseInput.getExpenses();
            if (expenses == null || expenses.size() == 0) {
                logger.info("No expenses found!");
            }
            else {
                logger.info("== Expenses ==");
                for (ExpenseOutput output: expenseOutputs) {
                    output.putExpenses(expenses);
                }
            }
        }

        // 2: Upload budgets
        logger.info("");
        if (cmd.hasOption("b")) {
            logger.info("== Budgets ==");
            BudgetHcpOutput budgetHcpOutput = new BudgetHcpOutput(
                new BudgetConfig(),
                new HcpConfig(
                    cmd.getOptionValue("url"),
                    cmd.getOptionValue("user"),
                    cmd.getOptionValue("pass"),
                    cmd.getOptionValue("x")
                )
            );
            budgetHcpOutput.putBudgets();
        }
        else {
            logger.info("No budgets will be uploaded!");
        }

        logger.info("");
        logger.info("All done.");
    }
}
