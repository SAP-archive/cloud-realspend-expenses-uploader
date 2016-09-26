package com.sap.expenseuploader.budgets;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sap.expenseuploader.config.budget.BudgetConfig;
import com.sap.expenseuploader.config.HcpConfig;
import com.sap.expenseuploader.model.BudgetEntry;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.management.relation.RoleNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.sap.expenseuploader.config.HcpConfig.getBodyFromResponse;

/**
 * Uploads the budgets, which are stored in the budgets.json file,
 * to the HCP realspend backend
 */
public class BudgetHcpOutput
{
    private final Logger logger = LogManager.getLogger(this.getClass());

    BudgetConfig budgetConfig;
    HcpConfig hcpConfig;

    private Map<String, Long> tagGroupIds = new HashMap<>();
    private Map<String, Map<String, Long>> tagNameIds = new HashMap<>();

    public BudgetHcpOutput( BudgetConfig budgetConfig, HcpConfig hcpConfig )
    {
        this.budgetConfig = budgetConfig;
        this.hcpConfig = hcpConfig;
    }

    public void putBudgets()
            throws IOException, URISyntaxException, ParseException, RoleNotFoundException {
        this.fillTagIdMaps();

        for (String user: budgetConfig.getBudgetUserList()) {

            logger.info("Uploading budgets for tags of user " + user);

            // Upload tag budgets
            Map<String, Map<String, List<BudgetEntry>>> userTagGroups = budgetConfig.getTagBudgetsOfUser(user);
            for( String userTagGroup : userTagGroups.keySet() ) {
                Map<String, List<BudgetEntry>> entries = userTagGroups.get(userTagGroup);
                putTagBudgets(userTagGroup, user, entries);
            }

            logger.info("Uploading budgets for master data of user " + user);

            Map<String, List<BudgetEntry>> masterDataBudgets;

            // Upload account budgets
            masterDataBudgets = budgetConfig.getMasterDataBudgetsOfUser(user, "account");
            putMasterDataBudgets("account", user, masterDataBudgets);

            // Upload account node budgets
            masterDataBudgets = budgetConfig.getMasterDataBudgetsOfUser(user, "accountnode");
            putMasterDataBudgets("account/hierarchy/node", user, masterDataBudgets);

            // Upload cost center budgets
            masterDataBudgets = budgetConfig.getMasterDataBudgetsOfUser(user, "costcenter");
            putMasterDataBudgets("cost-center", user, masterDataBudgets);

            // Upload cost center node budgets
            masterDataBudgets = budgetConfig.getMasterDataBudgetsOfUser(user, "costcenternode");
            putMasterDataBudgets("costcenternode", user, masterDataBudgets);

            // Upload internal order budgets
            masterDataBudgets = budgetConfig.getMasterDataBudgetsOfUser(user, "internalorder");
            putMasterDataBudgets("internal-order", user, masterDataBudgets);
        }
    }

    private void fillTagIdMaps()
        throws URISyntaxException, IOException, ParseException
    {
        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/tagging/dimension");
        Response response = this.hcpConfig.withOptionalProxy(Request.Get(uriBuilder.build())).execute();

        // Parse JSON
        String responseAsString = response.returnContent().toString();
        logger.debug("Got tagging dimensions: " + responseAsString);
        JSONParser parser = new JSONParser();
        JSONObject propertyMap = (JSONObject) parser.parse(responseAsString);

        for( JSONObject jsonObject : (Iterable<JSONObject>) propertyMap.get("dimensions") ) {
            long tagGroupId = (long) jsonObject.get("id");
            String tagGroupName = (String) jsonObject.get("name");
            this.tagGroupIds.put(tagGroupName, tagGroupId);
            Map<String, Long> tagGroupValuesMap = new HashMap<>();
            JSONArray tagGroupValues = (JSONArray) jsonObject.get("values");
            for( JSONObject tagObject : (Iterable<JSONObject>) tagGroupValues ) {
                tagGroupValuesMap.put((String) tagObject.get("name"), (Long) tagObject.get("id"));
            }
            this.tagNameIds.put(tagGroupName, tagGroupValuesMap);
        }
    }

    private void putTagBudgets( String tagGroupName, String user, Map<String, List<BudgetEntry>> entries )
            throws URISyntaxException, IOException, RoleNotFoundException {
        if( entries.isEmpty() ) {
            logger.debug("Nothing to to here ...");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("user", user);
        JsonArray budgets = new JsonArray();
        for( String tagName : entries.keySet() ) {
            // TODO create the missing tags in the API
            if (!this.tagNameIds.containsKey(tagGroupName)) {
                continue;
            }
            if (!this.tagNameIds.get(tagGroupName).containsKey(tagName)) {
                continue;
            }
            long tagId = this.tagNameIds.get(tagGroupName).get(tagName);
            for( BudgetEntry entry : entries.get(tagName) ) {
                JsonObject budget = new JsonObject();
                budget.addProperty("id", tagId);
                //budget.addProperty("name", tagName);
                budget.addProperty("amount", entry.amount);
                budget.addProperty("currency", entry.currency);
                budget.addProperty("year", entry.year);
                budgets.add(budget);
            }
        }
        if (budgets.size() == 0) {
            logger.debug("Nothing to do here ...");
            return;
        }
        payload.add("budgets", budgets);

        if( !this.tagGroupIds.containsKey(tagGroupName) ) {
            logger.debug("Key " + tagGroupName + " does not exist in tag groups");
            return;
        }

        long tagGroupId = this.tagGroupIds.get(tagGroupName);
        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/budget/dimension/" + tagGroupId);
        Request request = Request.Put(uriBuilder.build())
            .addHeader("x-csrf-token", this.hcpConfig.getCsrfToken())
            .bodyString(payload.toString(), ContentType.APPLICATION_JSON);
        HttpResponse response = this.hcpConfig.withOptionalProxy(request).execute().returnResponse();

        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode == 200 ) {
            logger.info(String.format("Successfully uploaded %s tag budgets for user %s",
                entries.size(),
                user));
            logger.debug("URL was: " + uriBuilder.build());
            logger.debug("Payload was: " + payload.toString());
        } else {
            logger.error(String.format("Got http code %s while uploading %s tag budgets for user %s",
                statusCode,
                entries.size(),
                user));
            logger.error("URL was: " + uriBuilder.build());
            logger.error("Payload was: " + payload.toString());
            logger.error("Error is: " + getBodyFromResponse(response));
        }
    }

    private void putMasterDataBudgets( String endpoint, String user, Map<String, List<BudgetEntry>> entries )
            throws URISyntaxException, IOException, RoleNotFoundException {
        if( entries.isEmpty() ) {
            logger.debug("Nothing to do for endpoint " + endpoint + " ...");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("user", user);
        JsonArray budgets = new JsonArray();
        for( String masterDataName : entries.keySet() ) {
            for( BudgetEntry entry : entries.get(masterDataName) ) {
                JsonObject budget = new JsonObject();
                budget.addProperty("name", masterDataName);
                budget.addProperty("amount", entry.amount);
                budget.addProperty("currency", entry.currency);
                budget.addProperty("year", entry.year);
                budgets.add(budget);
            }
        }
        payload.add("budgets", budgets);

        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/budget/" + endpoint);
        Request request = Request.Put(uriBuilder.build())
            .addHeader("x-csrf-token", this.hcpConfig.getCsrfToken())
            .bodyString(payload.toString(), ContentType.APPLICATION_JSON);
        HttpResponse response = this.hcpConfig.withOptionalProxy(request).execute().returnResponse();

        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode == 200 ) {
            int count = 0;
            for (String masterDataName: entries.keySet()) {
                count += entries.get(masterDataName).size();
            }
            logger.info(String.format("Successfully uploaded %s master data budgets for user %s",
                count, user));
            logger.debug("URL was: " + uriBuilder.build());
            logger.debug("Payload was: " + payload.toString());
        } else {
            logger.error(String.format("Got http code %s while uploading %s master data budgets for user %s",
                statusCode,
                entries.size(),
                user));
            logger.error("URL was: " + uriBuilder.build());
            logger.error("Payload was: " + payload.toString());
            logger.error("Error is: " + getBodyFromResponse(response));
        }
    }

}
