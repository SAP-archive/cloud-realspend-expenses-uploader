package com.sap.expenseuploader.config.costcenter;

import com.sap.expenseuploader.Helper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class JsonCostCenterConfig extends CostCenterConfig
{
    private final static String COSTCENTERS_JSON = "costcenters.json";

    private final Logger logger = LogManager.getLogger(this.getClass());

    public JsonCostCenterConfig()
            throws IOException, ParseException
    {
        this(COSTCENTERS_JSON);
    }

    public JsonCostCenterConfig( String path )
            throws IOException, ParseException
    {
        logger.debug("Reading cost centers for each user from JSON");

        // TODO test for duplicate cost centers and users

        // Parse JSON
        JSONParser parser = new JSONParser();
        JSONObject userMap = (JSONObject) parser.parse(new FileReader(path));
        for( Object user : userMap.keySet() ) {
            userCostCenters.put((String) user, new ArrayList<String>());
            for( Object costCenter : (JSONArray) userMap.get(user) ) {
                userCostCenters.get(user).add(Helper.stripLeadingZeros((String) costCenter));
            }
        }

        // Compute costCenterUsers
        for( String user : userCostCenters.keySet() ) {
            for( Object costCenterObj : userCostCenters.get(user) ) {
                String costCenter = (String) costCenterObj;
                if( !costCenterUsers.containsKey(costCenter) ) {
                    costCenterUsers.put(costCenter, new ArrayList<String>());
                }
                costCenterUsers.get(costCenter).add(user);
            }
        }
    }
}
