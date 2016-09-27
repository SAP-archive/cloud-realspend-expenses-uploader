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

    /**
     * this map stores tag groups along with their IDs in the realspend api
     * [ KEY , VALUE ] = [ tagGroupName , tagGroupID ]
     * <p>
     * NOTE: all tag group names are stored lower case strings.
     */
    private Map<String, Long> tagGroupIds = new HashMap<>();

    /**
     * this map stores the tags belonging to each tag group
     * [ KEY , VALUE ] = [ tagGroupName , [ tagName , tagID ] ]
     * <p>
     * NOTE: all tag names and tag group names are stored in lower case strings.
     */
    private Map<String, Map<String, Long>> tagNameIds = new HashMap<>();

    public BudgetHcpOutput( BudgetConfig budgetConfig, HcpConfig hcpConfig )
    {
        this.budgetConfig = budgetConfig;
        this.hcpConfig = hcpConfig;
    }

    public void putBudgets()
        throws IOException, URISyntaxException, ParseException, RoleNotFoundException
    {
        this.fillTagIdMaps();

        for( String user : budgetConfig.getBudgetUserList() ) {

            logger.info("Uploading budgets for tags of user " + user);

            // Upload tag budgets
            Map<String, Map<String, List<BudgetEntry>>> userTagGroups = budgetConfig.getTagBudgetsOfUser(user);
            for( String userTagGroup : userTagGroups.keySet() ) {
                Map<String, List<BudgetEntry>> entries = userTagGroups.get(userTagGroup);
                putTagBudgets(userTagGroup, user, entries);
            }

            logger.info("Uploading budgets for master data of user " + user);

            Map<String, List<BudgetEntry>> masterDataBudgets;

            // Upload overall budgets
            // TODO

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
            putMasterDataBudgets("cost-center/hierarchy/node", user, masterDataBudgets);

            // Upload internal order budgets
            masterDataBudgets = budgetConfig.getMasterDataBudgetsOfUser(user, "internalorder");
            putMasterDataBudgets("internal-order", user, masterDataBudgets);
        }
    }

    private void fillTagIdMaps()
        throws URISyntaxException, IOException, ParseException
    {
        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/tagging/dimension");
        Request request = Request.Get(uriBuilder.build())
                .addHeader("Authorization", "Basic " + this.hcpConfig.buildAuthString());
        HttpResponse response = this.hcpConfig.withOptionalProxy(request).execute().returnResponse();

        // Check response
        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode != 200 ) {
            logger.error(String.format(
                "Got http code %s while reading tags for user %s",
                statusCode, this.hcpConfig.getHcpUser())
            );
            logger.error("URL was: " + uriBuilder.build());
            logger.error("Body is: " + getBodyFromResponse(response));
            throw new IOException("Unable to read tags from HCP");
        }

        // Parse JSON
        String responseAsString = this.hcpConfig.getBodyFromResponse(response);
        logger.debug("Got tagging dimensions: " + responseAsString);
        JSONParser parser = new JSONParser();
        JSONObject propertyMap = (JSONObject) parser.parse(responseAsString);

        for( JSONObject jsonObject : (Iterable<JSONObject>) propertyMap.get("dimensions") ) {
            long tagGroupId = (long) jsonObject.get("id");
            String tagGroupName = (String) jsonObject.get("name");
            this.tagGroupIds.put(tagGroupName.toLowerCase(), tagGroupId);
            Map<String, Long> tagGroupValuesMap = new HashMap<>();
            JSONArray tagGroupValues = (JSONArray) jsonObject.get("values");
            for( JSONObject tagObject : (Iterable<JSONObject>) tagGroupValues ) {
                String tagName = (String) tagObject.get("name");
                tagGroupValuesMap.put(tagName.toLowerCase(), (Long) tagObject.get("id"));
            }
            this.tagNameIds.put(tagGroupName.toLowerCase(), tagGroupValuesMap);
        }
    }

    private void putTagBudgets( String tagGroupName, String user, Map<String, List<BudgetEntry>> entries )
        throws URISyntaxException, IOException, RoleNotFoundException, ParseException
    {
        if( entries.isEmpty() ) {
            logger.debug("No budgets to put ...");
            return;
        }

        JsonObject payload = new JsonObject();
        payload.addProperty("user", user);
        JsonArray budgets = new JsonArray();
        for( String tagName : entries.keySet() ) {
            // create the tag if it doesn't exist
            validateTagExistence(tagGroupName, tagName);

            long tagId = this.tagNameIds.get(tagGroupName.toLowerCase()).get(tagName.toLowerCase());
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
        if( budgets.size() == 0 ) {
            logger.debug("Nothing to do here ...");
            return;
        }
        payload.add("budgets", budgets);

        long tagGroupId = this.tagGroupIds.get(tagGroupName.toLowerCase());
        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/budget/dimension/" + tagGroupId);
        Request request = Request.Put(uriBuilder.build())
            .addHeader("x-csrf-token", this.hcpConfig.getCsrfToken())
            .bodyString(payload.toString(), ContentType.APPLICATION_JSON);
        HttpResponse response = this.hcpConfig.withOptionalProxy(request).execute().returnResponse();

        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode == 200 ) {
            logger.info(String.format("Successfully uploaded %s tag budgets for user %s", entries.size(), user));
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

    private void validateTagExistence( String tagGroupName, String tagName )
        throws URISyntaxException, IOException, RoleNotFoundException, ParseException
    {
        // creating the new tag group, will be empty with no related values
        if( !this.tagGroupIds.containsKey(tagGroupName.toLowerCase()) ) {
            long tagGroupID = createTagGroup(tagGroupName);
            this.tagGroupIds.put(tagGroupName.toLowerCase(), tagGroupID);
            this.tagNameIds.put(tagGroupName.toLowerCase(), new HashMap<String, Long>());
        }

        // creating the new tag
        if( !this.tagNameIds.get(tagGroupName.toLowerCase()).containsKey(tagName.toLowerCase()) ) {
            long tagID = createTag(tagGroupName, tagName);

            Map<String, Long> groupTagsMap = new HashMap<>();
            groupTagsMap.put(tagName.toLowerCase(), tagID);
            this.tagNameIds.put(tagGroupName.toLowerCase(), groupTagsMap);
        }
    }

    /**
     * creates a new tag on HCP and its tag group (dimension) if required
     *
     * @param tagGroupName
     * @param tagName
     * @return tagID
     * @throws URISyntaxException
     * @throws IOException
     * @throws RoleNotFoundException
     * @throws ParseException
     */
    private long createTag( String tagGroupName, String tagName )
        throws URISyntaxException, IOException, RoleNotFoundException, ParseException
    {
        logger.info("creating the tag " + tagName + " that belongs to group " + tagGroupName);

        // example:
        // {
        //   "dimensionValues": [
        //      {
        //          "dimensionName": "Region",
        //          "name": "East"
        //      }
        //    ]
        // }

        JsonObject tagJson = new JsonObject();
        tagJson.addProperty("dimensionName", tagGroupName);
        tagJson.addProperty("name", tagName);
        JsonArray dimensionValuesArray = new JsonArray();
        dimensionValuesArray.add(tagJson);
        JsonObject payload = new JsonObject();
        payload.add("dimensionValues", dimensionValuesArray);

        logger.info("payload of tag creation " + payload);

        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/tagging/values/");
        Request request = Request.Put(uriBuilder.build())
            .addHeader("x-csrf-token", this.hcpConfig.getCsrfToken())
            .bodyString(payload.toString(), ContentType.APPLICATION_JSON);
        Response response = this.hcpConfig.withOptionalProxy(request).execute();
        String responseAsString = response.returnContent().toString();

        JSONParser parser = new JSONParser();
        JSONObject propertyMap = (JSONObject) parser.parse(responseAsString);
        JSONArray responseDimensionValues = (JSONArray) propertyMap.get("dimensionValues");
        JSONObject createdDimensionValue = (JSONObject) responseDimensionValues.get(0);

        return (long) createdDimensionValue.get("id");
    }

    /**
     * creates a tag group (dimension) on realspend API, with empty dimension values
     *
     * @param tagGroupName
     * @return the id of the created dimension
     * @throws URISyntaxException
     * @throws IOException
     * @throws RoleNotFoundException
     * @throws ParseException
     */
    private long createTagGroup( String tagGroupName )
        throws URISyntaxException, IOException, RoleNotFoundException, ParseException
    {
        logger.info("creating the new tag group (dimension) " + tagGroupName);
        // example:
        // {
        //   "dimensions": [
        //      {
        //          "name": "Region"
        //      }
        //   ]
        // }

        JsonObject tagGroupJson = new JsonObject();
        tagGroupJson.addProperty("name", tagGroupName);
        JsonArray dimensionsArray = new JsonArray();
        dimensionsArray.add(tagGroupJson);
        JsonObject payload = new JsonObject();
        payload.add("dimensions", dimensionsArray);

        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/tagging/dimension/");
        Request request = Request.Put(uriBuilder.build())
            .addHeader("x-csrf-token", this.hcpConfig.getCsrfToken())
            .bodyString(payload.toString(), ContentType.APPLICATION_JSON);
        Response response = this.hcpConfig.withOptionalProxy(request).execute();
        String responseAsString = response.returnContent().toString();

        JSONParser parser = new JSONParser();
        JSONObject propertyMap = (JSONObject) parser.parse(responseAsString);
        JSONArray responseDimensions = (JSONArray) propertyMap.get("dimensions");
        JSONObject createdDimension = (JSONObject) responseDimensions.get(0);

        return (long) createdDimension.get("id");
    }

    private void putMasterDataBudgets( String endpoint, String user, Map<String, List<BudgetEntry>> entries )
        throws URISyntaxException, IOException, RoleNotFoundException
    {
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
            for( String masterDataName : entries.keySet() ) {
                count += entries.get(masterDataName).size();
            }
            logger.info(String.format("Successfully uploaded %s master data budgets for user %s", count, user));
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
