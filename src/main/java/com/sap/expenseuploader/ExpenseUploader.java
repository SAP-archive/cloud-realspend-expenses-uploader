package com.sap.expenseuploader;

import com.sap.expenseuploader.budgets.BudgetHcpOutput;
import com.sap.expenseuploader.config.ExpenseInputConfig;
import com.sap.expenseuploader.config.HcpConfig;
import com.sap.expenseuploader.config.budget.ExcelBudgetConfig;
import com.sap.expenseuploader.config.costcenter.ExcelCostCenterConfig;
import com.sap.expenseuploader.expenses.input.ErpInput;
import com.sap.expenseuploader.expenses.input.ExcelInput;
import com.sap.expenseuploader.expenses.input.ExpenseInput;
import com.sap.expenseuploader.expenses.output.CliOutput;
import com.sap.expenseuploader.expenses.output.ExcelOutput;
import com.sap.expenseuploader.expenses.output.ExpenseHcpOutput;
import com.sap.expenseuploader.expenses.output.ExpenseOutput;
import com.sap.expenseuploader.model.Expense;
import org.apache.commons.cli.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import javax.management.relation.RoleNotFoundException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the main entry point for the program
 */
public class ExpenseUploader
{
    private static final String DEFAULT_CONFIG_PATH = "config.xlsx";

    private static final Logger logger = LogManager.getLogger(ExpenseUploader.class);

    public static void main( String[] args )
        throws
        IOException,
        org.json.simple.parser.ParseException,
        ParseException,
        java.text.ParseException,
        RoleNotFoundException,
        URISyntaxException,
        InvalidFormatException
    {
        logger.info("--- Expense & Budget Uploader ---");

        Options options = new Options();

        // General options
        options.addOption("h", "help", false, "print this message");
        options.addOption("c", "config", true, "path to configuration file (default is " + DEFAULT_CONFIG_PATH + ")");
        options.addOption("b", "budgets", false, "upload budgets to HCP (optional, needs HCP URL)");

        // HCP options
        options.addOption("url",
            "hcp_url",
            true,
            "account-specific URL of HCP, e.g. https://bmsfin-<accountId>.hanatrial.ondemand.com/core/api/v1");
        options.addOption("user", "hcp_user", true, "your hcp username (optional, will prompt), e.g. p12345trial");
        options.addOption("pass", "hcp_password", true, "your hcp password (optional, will prompt)");
        options.addOption("x", "hcp_proxy", true, "proxy server (optional), e.g. example.com:8080");
        options.addOption("r", "resume", false, "retry uploading failed expense uploading requests from previous run");

        // Options for Expenses
        options.addOption("in_erp",
            "input_erp",
            true,
            "id of the erp system used for expense input, default is 'system'");
        options.addOption("in_xls", "input_xls", true, "path to expense input excel file");
        options.addOption("out_cli", "output_cli", false, "write expenses to command line");
        options.addOption("out_hcp", "output_hcp", false, "write expenses to HCP");
        options.addOption("out_xls", "output_xls", true, "write expenses to excel file (path needed)");
        options.addOption("ca", "controlling-area", true, "controlling area, e.g. 0001");
        options.addOption("f", "from", true, "lower posting date in YYYYMMDD format");
        options.addOption("t", "to", true, "higher posting date in YYYYMMDD format (optional)");
        options.addOption("p", "period", true, "period (optional), e.g. 001");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse(options, args);

        // Print help
        if( cmd.hasOption("h") ) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(" ", options);
            return;
        }

        // Look for config file
        String configPath = DEFAULT_CONFIG_PATH;
        if (cmd.hasOption("c")) {
            configPath = cmd.getOptionValue("c");
        }
        File configFile = new File(configPath);
        if (!configFile.exists() || configFile.isDirectory()) {
            logger.error("Unable to find configuration file at " + configFile.getAbsolutePath());
            System.exit(1);
        }

        // Prepare HCP config
        HcpConfig hcpConfig = null;
        if (cmd.hasOption("url")) {
            hcpConfig = new HcpConfig(
                cmd.getOptionValue("url"),
                cmd.getOptionValue("user"),
                cmd.getOptionValue("pass"),
                cmd.getOptionValue("x"),
                cmd.hasOption("r")
            );
        }

        // Prepare config for expenses, create inputs and outputs
        ExpenseInput expenseInput = null;
        List<ExpenseOutput> expenseOutputs = new ArrayList<>();
        if( cmd.hasOption("in_erp") ) {
            expenseInput = new ErpInput(
                new ExpenseInputConfig(
                    cmd.getOptionValue("in_erp"),
                    cmd.getOptionValue("ca"),
                    cmd.getOptionValue("f"),
                    cmd.getOptionValue("t"),
                    cmd.getOptionValue("p")
                ),
                new ExcelCostCenterConfig(configPath)
            );
        }
        if( cmd.hasOption("in_xls") ) {
            if( expenseInput != null ) {
                logger.error("More than one input defined!");
                System.exit(1);
            }
            expenseInput = new ExcelInput(cmd.getOptionValue("in_xls"));
        }
        if( cmd.hasOption("out_cli") ) {
            expenseOutputs.add(new CliOutput());
        }
        if( cmd.hasOption("out_hcp") ) {
            if( !cmd.hasOption("url") ) {
                logger.error("Please specify the HCP URL to upload expenses");
                System.exit(1);
            }

            expenseOutputs.add(
                new ExpenseHcpOutput(
                    hcpConfig,
                    new ExcelCostCenterConfig(configPath)
                )
            );
        }
        if( cmd.hasOption("out_xls") ) {
            expenseOutputs.add(new ExcelOutput(cmd.getOptionValue("out_xls")));
        }
        if( expenseInput == null && expenseOutputs.size() == 0 ) { // No expenses
            logger.info("No inputs or outputs defined, skipping expenses");
        } else if( expenseInput != null && expenseOutputs.size() != 0 ) { // Correct number of in- and outputs
            if( !cmd.hasOption("f") || !cmd.hasOption("ca") ) {
                logger.error("Please specify expense parameters 'from' and 'controlling-area'");
                System.exit(1);
            }
        } else {
            logger.error("Please specify for expenses either no inputs and no outputs"
                + " or exactly one input and one or more outputs.");
            System.exit(1);
        }

        // Prepare budgets
        if( cmd.hasOption("b") && !cmd.hasOption("url") ) {
            logger.error("Please specify the HCP URL to upload budgets");
            System.exit(1);
        }

        // Do the work
        // 1: Upload expenses
        if( expenseInput != null ) {
            logger.info("");
            List<Expense> expenses = expenseInput.getExpenses();
            if( expenses == null || expenses.size() == 0 ) {
                logger.info("No expenses found in the given input source!");
            } else {
                logger.info("== Uploading Expenses ==");
                for( ExpenseOutput output : expenseOutputs ) {
                    output.putExpenses(expenses);
                }
            }
        }

        // 2: Upload budgets
        logger.info("");
        if( cmd.hasOption("b") ) {
            logger.info("== Uploading Budgets ==");
            BudgetHcpOutput budgetHcpOutput = new BudgetHcpOutput(
                new ExcelBudgetConfig(configPath),
                hcpConfig
            );
            budgetHcpOutput.putBudgets();
        } else {
            logger.info("No budgets will be uploaded! Consider using the option 'budgets' if they are required.");
        }

        logger.info("");
        logger.info("All done.");
    }
}
