package com.sap.expenseuploader.expenses.output;

import com.google.gson.*;
import com.sap.expenseuploader.config.CostcenterConfig;
import com.sap.expenseuploader.config.HcpConfig;
import com.sap.expenseuploader.model.Expense;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static com.sap.expenseuploader.config.HcpConfig.getBodyFromResponse;

/**
 * This is the most common method of output, to upload expenses to the HCP realspend
 * backend.
 */
public class ExpenseHcpOutput implements ExpenseOutput
{
    private final Logger logger = LogManager.getLogger(this.getClass());

    public static final Path REQ_DUMP_FOLDER = Paths.get("requests");

    private HcpConfig hcpConfig;
    private CostcenterConfig costcenterConfig;

    public ExpenseHcpOutput( HcpConfig hcpConfig, CostcenterConfig costcenterConfig )
    {
        this.hcpConfig = hcpConfig;
        this.costcenterConfig = costcenterConfig;
    }

    @Override
    public void putExpenses( List<Expense> expenses )
    {
        logger.info("Writing expenses to HCP at " + this.hcpConfig.getHcpUrl());

        deleteRequestFolder();

        try {
            // Fetch CSRF token and authenticate
            String csrfToken = this.hcpConfig.getCsrfToken();

            // Upload
            for( String user : this.costcenterConfig.getCostCenterUserList() ) {
                List<String> costCenters = this.costcenterConfig.getCostCenters(user);
                List<Expense> userExpenses = new ArrayList<>();
                for( Expense expense : expenses ) {
                    if( expense.isInCostCenter(costCenters) ) {
                        userExpenses.add(expense);
                    }
                }
                if( userExpenses.isEmpty() ) {
                    logger.info("No expenses to put for user " + user);
                    continue;
                }
                uploadExpenses(userExpenses, user, csrfToken);
            }
        }
        catch( Exception e ) {
            throw new RuntimeException("Failed to post expenses", e);
        }
    }

    private void deleteRequestFolder()
    {
        try {
            logger.info("Deleting old request folder");
            Files.walkFileTree(REQ_DUMP_FOLDER, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
                    throws IOException
                {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory( Path dir, IOException exc )
                    throws IOException
                {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }

            });
        }
        catch( Exception e ) {
            logger.error("Failed to delete " + REQ_DUMP_FOLDER, e);
        }
    }

    private long getExpenseCount()
        throws URISyntaxException, IOException, ParseException
    {
        // TODO this does not show the expenses of other users, so on-behalf postings can not be checked.

        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/expense/count");
        Request request = Request.Get(uriBuilder.build());
        HttpResponse response = this.hcpConfig.withOptionalProxy(request).execute().returnResponse();

        // Check response
        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode == 200 ) {
            // Parse JSON
            String responseAsString = getBodyFromResponse(response);
            JSONParser parser = new JSONParser();
            JSONObject propertyMap = (JSONObject) parser.parse(responseAsString);
            return (Long) propertyMap.get("count");
        } else {
            logger.error(String.format("Got http code %s while uploading %s expenses for user %s",
                statusCode,
                this.hcpConfig.getHcpUser()));
            logger.error("URL was: " + uriBuilder.build());
            logger.error("Error is: " + getBodyFromResponse(response));
            throw new IOException("Unable to get count of expenses");
        }
    }

    private void uploadExpenses( List<Expense> expenses, String user, String csrfToken )
        throws URISyntaxException, IOException
    {
        // Create JSON payload
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
        JsonArray expensesAsJson = (JsonArray) gson.toJsonTree(expenses);
        for( JsonElement e : expensesAsJson ) {
            // Add approver to each expense
            JsonObject expenseAsJson = (JsonObject) e;
            expenseAsJson.addProperty("approver", this.hcpConfig.getHcpUser());
        }
        JsonObject payload = new JsonObject();
        payload.add("expenses", expensesAsJson);
        payload.addProperty("user", user);
        dumpRequest(user, new GsonBuilder().setPrettyPrinting().create().toJson(payload));

        // TODO delta merge: Compare expenses with what's already there
        // Blocked until we have on-behalf lookup
        // The Helper class already has a method to compare new and existing expenses

        // Upload
        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/expense");
        Request request = Request.Post(uriBuilder.build())
            .addHeader("x-csrf-token", csrfToken)
            .bodyString(payload.toString(), ContentType.APPLICATION_JSON);

        logger.info(String.format("Posting %s expenses for user %s", expenses.size(), user));

        final long start = System.currentTimeMillis();
        HttpResponse response = this.hcpConfig.withOptionalProxy(request).execute().returnResponse();
        final long duration = System.currentTimeMillis() - start;

        // Check response
        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode == 200 ) {
            logger.info(String.format("Successfully uploaded %s expenses for user %s (took %s sec)",
                expenses.size(),
                user,
                duration / 1000));
        } else {
            logger.error(String.format("Got http code %s while uploading %s expenses for user %s",
                statusCode,
                expenses.size(),
                user));
            logger.error("URL was: " + uriBuilder.build());
            logger.error("Error is: " + getBodyFromResponse(response));
            System.exit(1);
        }
    }

    private void dumpRequest( final String key, final String s )
    {
        try {
            if( !Files.isDirectory(REQ_DUMP_FOLDER) ) {
                Files.createDirectory(REQ_DUMP_FOLDER);
            }
        }
        catch( Exception e ) {
            logger.error("Failed to create " + REQ_DUMP_FOLDER);
            return;
        }

        final File file = new File(REQ_DUMP_FOLDER.toFile(), key + ".json");
        try( PrintWriter writer = new PrintWriter(file) ) {
            writer.write(s);
        }
        catch( Exception e ) {
            throw new RuntimeException(e);
        }
    }
}
