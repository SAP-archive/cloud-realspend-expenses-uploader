package com.sap.expenseuploader.config.budget;

import com.sap.expenseuploader.model.BudgetEntry;

import java.util.*;

public abstract class BudgetConfig
{
    // Map from the user to the overall budgets
    // user -> entries
    protected final Map<String, List<BudgetEntry>> userOverallBudgets = new HashMap<>();

    // Map from user to the budgets of tags
    // user -> tag group -> tag name -> entries
    protected final Map<String, Map<String, Map<String, List<BudgetEntry>>>> userTagBudgets = new HashMap<>();

    // Map from user to the budgets of master data
    // user -> master data type -> master data name -> entries
    protected final Map<String, Map<String, Map<String, List<BudgetEntry>>>> userMasterDataBudgets = new HashMap<>();

    public List<BudgetEntry> getOverallBudgetsOfUser(String user)
    {
        if (!userOverallBudgets.containsKey(user)) {
            // Immutable empty map
            return Collections.emptyList();
        }
        return userOverallBudgets.get(user);
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
     * @param masterDataType
     * @return data or an empty list
     */
    public Map<String, List<BudgetEntry>> getMasterDataBudgetsOfUser( String user, String masterDataType )
    {
        if( !userMasterDataBudgets.containsKey(user) ) {
            // Immutable empty map
            return Collections.emptyMap();
        }
        if( !userMasterDataBudgets.get(user).containsKey(masterDataType) ) {
            // Immutable empty map
            return Collections.emptyMap();
        }
        return userMasterDataBudgets.get(user).get(masterDataType);
    }

    public List<String> getBudgetUserList()
    {
        Set<String> users = new HashSet<>(userMasterDataBudgets.keySet());
        users.addAll(userTagBudgets.keySet());
        List<String> result = new ArrayList<>(users);
        Collections.sort(result);
        return result;
    }

    @Override
    public String toString() {
        return "Master data: " + userMasterDataBudgets.toString()
                + ", Tags: " + userTagBudgets.toString()
                + ", Overall: " + userOverallBudgets.toString();
    }
}
