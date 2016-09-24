package com.sap.expenseuploader.config;

import com.sap.expenseuploader.model.BudgetEntry;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class BudgetConfig
{

    private final static String BUDGETS_JSON = "budgets.json";

    // Map from user to the budgets of tags
    // user -> tag group -> tag name -> entries
    private final Map<String, Map<String, Map<String, List<BudgetEntry>>>> userTagBudgets = new HashMap<>();

    // Map from user to the budgets of master data
    // user -> master data type -> master data name -> entries
    private final Map<String, Map<String, Map<String, List<BudgetEntry>>>> userMasterDataBudgets = new HashMap<>();

    public BudgetConfig()
        throws IOException, ParseException
    {
        this.readBudgetsFromJson(BUDGETS_JSON);
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

    public List<String> getBudgetUserList()
    {
        Set<String> users = new HashSet<>(userMasterDataBudgets.keySet());
        users.addAll(userTagBudgets.keySet());
        List<String> result = new ArrayList<>(users);
        Collections.sort(result);
        return result;
    }
}
