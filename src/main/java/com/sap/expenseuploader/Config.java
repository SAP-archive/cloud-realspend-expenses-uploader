package com.sap.expenseuploader;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import com.sap.expenseuploader.model.BudgetEntry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Stores all user configurations and preferences in one place. Including command-line parameters.
 */
public class Config
{
    // Command line parameters
    private String input;
    private String output;
    private String controllingArea;
    private String fromTime;
    private String toTime;
    private String period;
    private String hcpUser;
    private String hcpPass;
    private String proxy;
    private boolean withBudgets;

    // Map from user to list of cost centers
    private Map<String, List<String>> userCostCenters;

    // Map from cost centers to list of costCenterUsers
    private Map<String, List<String>> costCenterUsers;

    // Map from user to the budgets of tags
    // user -> tag group -> tag name -> entries
    private Map<String, Map<String, Map<String, List<BudgetEntry>>>> userTagBudgets;

    // Map from user to the budgets of master data
    // user -> master data type -> master data name -> entries
    private Map<String, Map<String, Map<String, List<BudgetEntry>>>> userMasterDataBudgets;

    public Config( String controllingArea, String fromTime, String toTime, String period, String hcpUser,
        String hcpPass, String proxy, String budgetsJsonPath, String costCentersJsonPath, boolean withBudgets )
        throws IOException, ParseException
    {
        this.controllingArea = controllingArea;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.period = period;
        this.hcpUser = hcpUser;
        this.hcpPass = hcpPass;
        this.proxy = proxy;

        this.userCostCenters = new HashMap<>();
        this.costCenterUsers = new HashMap<>();
        this.userTagBudgets = new HashMap<>();
        this.userMasterDataBudgets = new HashMap<>();
        this.withBudgets = withBudgets;
        if( this.withBudgets ) {
            readBudgetsFromJson(budgetsJsonPath);
        }

        readCostCentersFromJson(costCentersJsonPath);
    }

    public String getInput()
    {
        return this.input;
    }

    public void setInput( String input )
    {
        this.input = input;
    }

    public void setOutput( String output )
    {
        this.output = output;
    }

    public String getOutput()
    {
        return this.output;
    }

    public String getControllingArea()
    {
        return this.controllingArea;
    }

    public String getFromTime()
    {
        return this.fromTime;
    }

    public String getToTime()
    {
        if( this.toTime == null ) {
            // If not set, set to yesterday
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            this.toTime = dateFormat.format(cal.getTime());
            System.out.println("Set to-time to " + this.toTime);
        }
        return this.toTime;
    }

    public boolean hasPeriod()
    {
        return this.period != null;
    }

    public String getPeriod()
    {
        return this.period;
    }

    public String getHcpUser()
    {
        if( this.hcpUser == null ) {
            // If this was not provided, show prompt
            if( System.console() == null ) {
                System.out.println("Unable to prompt for username!");
                return null;
            }
            this.hcpUser = System.console().readLine("HCP Username: ");
        }
        return this.hcpUser;
    }

    public String getHcpPass()
    {
        if( this.hcpPass == null ) {
            // If this was not provided, show prompt
            if( System.console() == null ) {
                System.out.println("Unable to prompt for password!");
                return null;
            }
            this.hcpPass = new String(System.console().readPassword("HCP Password: "));
        }
        return this.hcpPass;
    }

    public String getProxy()
    {
        return this.proxy;
    }

    private void readBudgetsFromJson( String path )
        throws IOException, ParseException
    {
        JSONParser parser = new JSONParser();
        JSONObject userMap = (JSONObject) parser.parse(new FileReader(path));
        for( Object userObject : userMap.keySet() ) {
            String user = (String) userObject;
            if( !userTagBudgets.containsKey(user) ) {
                userTagBudgets.put(user, new HashMap<String, Map<String, List<BudgetEntry>>>());
            }
            if( !userMasterDataBudgets.containsKey(user) ) {
                userMasterDataBudgets.put(user, new HashMap<String, Map<String, List<BudgetEntry>>>());
            }
            JSONObject groups = (JSONObject) userMap.get(user);
            for( Object groupObject : groups.keySet() ) {
                String group = (String) groupObject;
                JSONObject data = (JSONObject) groups.get(group);
                if( group.equalsIgnoreCase("tag") ) {
                    storeUserTagBudgets(user, data);
                } else {
                    storeUserMasterDataBudgets(user, data, group);
                }
            }
        }
    }

    // Called with a user and all tags
    private void storeUserTagBudgets( String user, JSONObject tags )
    {
        for( Object tagGroupObject : tags.keySet() ) {
            String tagGroup = (String) tagGroupObject;
            JSONObject tagNamesJson = (JSONObject) tags.get(tagGroup);
            for( Object tagNameObject : tagNamesJson.keySet() ) {
                String tagName = (String) tagNameObject;
                JSONArray entriesJson = (JSONArray) tagNamesJson.get(tagName);
                for( Object entryObject : entriesJson ) {
                    BudgetEntry entry = new BudgetEntry((JSONObject) entryObject);
                    Map<String, Map<String, List<BudgetEntry>>> tagGroups = userTagBudgets.get(user);
                    if( !tagGroups.containsKey(tagGroup) ) {
                        tagGroups.put(tagGroup, new HashMap<String, List<BudgetEntry>>());
                    }
                    Map<String, List<BudgetEntry>> tagNames = tagGroups.get(tagGroup);
                    if( !tagNames.containsKey(tagName) ) {
                        tagNames.put(tagName, new ArrayList<BudgetEntry>());
                    }
                    List<BudgetEntry> entries = tagNames.get(tagName);
                    entries.add(entry);
                }
            }
        }
    }

    // Called with a user and one type of master data
    private void storeUserMasterDataBudgets( String user, JSONObject masterDataJson, String masterDataType )
    {
        if( !userMasterDataBudgets.get(user).containsKey(masterDataType) ) {
            userMasterDataBudgets.get(user).put(masterDataType, new HashMap<String, List<BudgetEntry>>());
        }
        Map<String, List<BudgetEntry>> masterDataNames = userMasterDataBudgets.get(user).get(masterDataType);
        for( Object masterDataNameObject : masterDataJson.keySet() ) {
            String masterDataName = (String) masterDataNameObject;
            JSONArray entriesJson = (JSONArray) masterDataJson.get(masterDataName);
            for( Object entryObject : entriesJson ) {
                BudgetEntry entry = new BudgetEntry((JSONObject) entryObject);
                if( !masterDataNames.containsKey(masterDataName) ) {
                    masterDataNames.put(masterDataName, new ArrayList<BudgetEntry>());
                }
                List<BudgetEntry> entries = masterDataNames.get(masterDataName);
                entries.add(entry);
            }
        }
    }

    /**
     * Returns the tags of this user
     *
     * @param user
     * @return data or an empty list
     */
    public Map<String, Map<String, List<BudgetEntry>>> getTagBudgetsOfUser( String user )
    {
        if( !userTagBudgets.containsKey(user) ) {
            // Immutable empty map
            return Collections.emptyMap();
        }
        return userTagBudgets.get(user);
    }

    /**
     * Returns the master data of this user
     *
     * @param user
     * @param masterDataKey
     * @return data or an empty list
     */
    public Map<String, List<BudgetEntry>> getMasterDataBudgetsOfUser( String user, String masterDataKey )
    {
        if( !userMasterDataBudgets.containsKey(user) ) {
            // Immutable empty map
            return Collections.emptyMap();
        }
        if( !userMasterDataBudgets.get(user).containsKey(masterDataKey) ) {
            // Immutable empty map
            return Collections.emptyMap();
        }
        return userMasterDataBudgets.get(user).get(masterDataKey);
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
                userCostCenters.get(user).add((String) costCenter);
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

    public List<String> getUserList()
    {
        // TODO users are also in the budget config json
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

    // This method exists here so that we can mock it
    public JCoDestination getJcoDestination( String name )
        throws JCoException
    {
        return JCoDestinationManager.getDestination(name);
    }
}
