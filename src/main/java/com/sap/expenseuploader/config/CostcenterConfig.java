package com.sap.expenseuploader.config;

import com.sap.expenseuploader.Helper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CostcenterConfig
{

    private final static String COSTCENTERS_JSON = "costcenters.json";

    // Map from user to list of cost centers
    private Map<String, List<String>> userCostCenters = new HashMap<>();

    // Map from cost centers to list of costCenterUsers
    private Map<String, List<String>> costCenterUsers = new HashMap<>();

    public CostcenterConfig()
        throws IOException, ParseException
    {
        readCostCentersFromJson(COSTCENTERS_JSON);
    }

    private void readCostCentersFromJson( String path )
        throws IOException, ParseException
    {
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
            for( String costCenter : userCostCenters.get(user) ) {
                if( !costCenterUsers.containsKey(costCenter) ) {
                    costCenterUsers.put(costCenter, new ArrayList<String>());
                }
                costCenterUsers.get(costCenter).add(user);
            }
        }
    }

    public List<String> getCostCenterList()
    {
        List<String> result = new ArrayList<>(costCenterUsers.keySet());
        Collections.sort(result);
        return result;
    }

    public List<String> getCostCenterUserList()
    {
        List<String> result = new ArrayList<>(userCostCenters.keySet());
        Collections.sort(result);
        return result;
    }

    public List<String> getCostCenters( String user )
    {
        return userCostCenters.get(user);
    }

    public List<String> getUsers( String costCenter )
    {
        return costCenterUsers.get(costCenter);
    }
}
