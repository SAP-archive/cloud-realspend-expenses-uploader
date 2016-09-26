package com.sap.expenseuploader.expenses.output;

import com.google.gson.*;
import com.sap.expenseuploader.config.HcpConfig;
import com.sap.expenseuploader.config.costcenter.CostCenterConfig;
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

import javax.management.relation.RoleNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
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

    private HcpConfig hcpConfig;
    private CostCenterConfig costCenterConfig;

    public ExpenseHcpOutput( HcpConfig hcpConfig, CostCenterConfig costCenterConfig)
    {
        this.hcpConfig = hcpConfig;
        this.costCenterConfig = costCenterConfig;
    }

    @Override
    public void putExpenses( List<Expense> expenses )
    {
        logger.info("Writing expenses to HCP at " + this.hcpConfig.getHcpUrl());

        try {
            // Upload
            for( String user : this.costCenterConfig.getCostCenterUserList() ) {
                List<String> costCenters = this.costCenterConfig.getCostCenters(user);
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
                uploadExpenses(userExpenses, user);
            }
        }
        catch( Exception e ) {
            logger.error(e);
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

    private void uploadExpenses( List<Expense> expenses, String user )
            throws URISyntaxException, IOException, RoleNotFoundException {
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
        logger.debug(new GsonBuilder().setPrettyPrinting().create().toJson(payload));

        // TODO delta merge: Compare expenses with what's already there
        // Blocked until we have on-behalf lookup
        // The Helper class already has a method to compare new and existing expenses

        // Upload
        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/expense");
        Request request = Request.Post(uriBuilder.build())
            .addHeader("x-csrf-token", this.hcpConfig.getCsrfToken())
            .bodyString(payload.toString(), ContentType.APPLICATION_JSON);

        logger.info(String.format("Posting %s expenses for user %s", expenses.size(), user));

        HttpResponse response = this.hcpConfig.withOptionalProxy(request).execute().returnResponse();

        // Check response
        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode == 200 ) {
            logger.info(String.format("Successfully uploaded %s expenses for user %s", expenses.size(), user));
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
}
