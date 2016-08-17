package com.sap.expenseuploader.expenses.output;

import com.google.gson.*;
import com.sap.expenseuploader.Config;
import com.sap.expenseuploader.model.Expense;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static com.sap.expenseuploader.Helper.getCsrfToken;
import static com.sap.expenseuploader.Helper.withOptionalProxy;

/**
 * This is the most common method of output, to upload expenses to the HCP realspend
 * backend.
 */
public class ExpenseHcpOutput extends AbstractOutput
{
    public ExpenseHcpOutput( Config config )
    {
        super(config);
        if( config.getOutput() == null || config.getOutput().isEmpty() ) {
            // This is here because the HTTP client has a hard to read error message
            System.out.println("Output URL not set!");
        }
    }

    @Override
    public boolean putExpenses( List<Expense> expenses )
    {
        try {
            // Get CSRF token and authenticate
            String csrfToken = getCsrfToken(this.config);

            // Print current count
            long expenseCount = getExpenseCount();
            System.out.println("Found " + expenseCount + " expenses currently in HCP");

            // Upload
            for( String user : this.config.getUserList() ) {
                List<String> costCenters = this.config.getCostCenters(user);
                List<Expense> userExpenses = new ArrayList<>();
                for( Expense expense : expenses ) {
                    if( expense.isInCostCenter(costCenters) ) {
                        userExpenses.add(expense);
                    }
                }
                if( userExpenses.isEmpty() ) {
                    System.out.println("No expenses for user " + user);
                    continue;
                }
                uploadExpenses(userExpenses, user, csrfToken);
            }

            // Print current count
            expenseCount = getExpenseCount();
            System.out.println("Found " + expenseCount + " expenses currently in HCP");
        }
        catch( Exception e ) {
            e.printStackTrace();
        }

        return true;
    }

    private long getExpenseCount()
        throws URISyntaxException, IOException, ParseException
    {
        URIBuilder uriBuilder = new URIBuilder(this.config.getOutput() + "/rest/expense/count");
        Response response = withOptionalProxy(this.config.getProxy(), Request.Get(uriBuilder.build())).execute();

        // Parse JSON
        String responseAsString = response.returnContent().toString();
        JSONParser parser = new JSONParser();
        JSONObject propertyMap = (JSONObject) parser.parse(responseAsString);
        return (Long) propertyMap.get("count");
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

        // TODO delta merge: Compare expenses with what's already there

        // Upload
        URIBuilder uriBuilder = new URIBuilder(this.config.getOutput() + "/rest/expense");
        Response response = withOptionalProxy(this.config.getProxy(),
            Request.Post(uriBuilder.build())
                .addHeader("x-csrf-token", csrfToken)
                .bodyString(payload.toString(), ContentType.APPLICATION_JSON)).execute();

        // Check response
        HttpResponse httpResponse = response.returnResponse();
        int statusCode = httpResponse.getStatusLine().getStatusCode();
        if( statusCode == 200 ) {
            System.out.println(String.format("Successfully uploaded %s expenses for user %s", expenses.size(), user));
        } else {
            System.out.println(String.format("Got http code %s while uploading %s expenses for user %s",
                statusCode,
                expenses.size(),
                user));
            System.out.println(httpResponse);
        }
    }
}
