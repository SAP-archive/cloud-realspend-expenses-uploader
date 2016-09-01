package com.sap.expenseuploader.expenses.output;

import com.google.gson.*;
import com.sap.expenseuploader.Config;
import com.sap.expenseuploader.model.Expense;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.sap.expenseuploader.Helper.getBodyFromResponse;
import static com.sap.expenseuploader.Helper.getCsrfToken;
import static com.sap.expenseuploader.Helper.withOptionalProxy;

/**
 * This is the most common method of output, to upload expenses to the HCP realspend
 * backend.
 */
public class ExpenseHcpOutput extends AbstractOutput
{
    private final Logger logger = LogManager.getLogger(this.getClass());

    public ExpenseHcpOutput( Config config )
    {
        super(config);
        if( config.getOutput() == null || config.getOutput().isEmpty() ) {
            // This is here because the HTTP client has a hard to read error message
            logger.error("Output URL not set!");
        }
    }

    @Override
    public boolean putExpenses( List<Expense> expenses )
    {
        try {
            // Get CSRF token and authenticate
            String csrfToken = getCsrfToken(this.config);

            // Print current count
            //long expenseCount = getExpenseCount();
            //logger.info("Found " + expenseCount + " expenses currently in HCP");

            // Upload
            for (String user : this.config.getCostCenterUserList()) {
                List<String> costCenters = this.config.getCostCenters(user);
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

            // Print current count
            //expenseCount = getExpenseCount();
            //logger.info("Found " + expenseCount + " expenses currently in HCP");

            // TODO this should have gone up because of the upload,
            // but the API does not let us check the expenses of other users
        }
        catch( Exception e ) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private long getExpenseCount()
        throws URISyntaxException, IOException, ParseException
    {
        // TODO this does not show the expenses of other users, so on-behalf postings can not be checked.

        URIBuilder uriBuilder = new URIBuilder(this.config.getOutput() + "/rest/expense/count");
        Request request = Request.Get(uriBuilder.build());
        HttpResponse response = withOptionalProxy(this.config.getProxy(), request).execute().returnResponse();

        // Check response
        int statusCode = response.getStatusLine().getStatusCode();
        if ( statusCode == 200 ) {
            // Parse JSON
            String responseAsString = getBodyFromResponse(response);
            JSONParser parser = new JSONParser();
            JSONObject propertyMap = (JSONObject) parser.parse(responseAsString);
            return (Long) propertyMap.get("count");
        }
        else {
            logger.error(String.format("Got http code %s while uploading %s expenses for user %s",
                    statusCode,
                    config.getHcpUser()));
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
            // TODO can't the API do that?
            JsonObject expenseAsJson = (JsonObject) e;
            expenseAsJson.addProperty("approver", this.config.getHcpUser());
        }
        JsonObject payload = new JsonObject();
        payload.add("expenses", expensesAsJson);
        payload.addProperty("user", user);
        logger.debug(payload.toString());

        // TODO delta merge: Compare expenses with what's already there

        // Upload
        URIBuilder uriBuilder = new URIBuilder(this.config.getOutput() + "/rest/expense");
        Request request = Request.Post(uriBuilder.build())
            .addHeader("x-csrf-token", csrfToken)
            .bodyString(payload.toString(), ContentType.APPLICATION_JSON);
        HttpResponse response = withOptionalProxy(this.config.getProxy(), request).execute().returnResponse();

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
        }
    }
}
