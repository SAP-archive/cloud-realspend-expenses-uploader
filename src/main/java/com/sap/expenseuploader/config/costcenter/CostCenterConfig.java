package com.sap.expenseuploader.config.costcenter;

import java.util.*;

public abstract class CostCenterConfig
{
    // Map from user to list of cost centers
    protected Map<String, ArrayList<String>> userCostCenters = new HashMap<>();

    // Map from cost centers to list of costCenterUsers
    protected Map<String, List<String>> costCenterUsers = new HashMap<>();

    /**
     * Returns the sorted list of all unique cost centers
     */
    public List<String> getCostCenterList()
    {
        List<String> result = new ArrayList<>(costCenterUsers.keySet());
        Collections.sort(result);
        return result;
    }

    /**
     * Returns the sorted list of all unique users
     */
    public List<String> getUserList()
    {
        List<String> result = new ArrayList<>(userCostCenters.keySet());
        Collections.sort(result);
        return result;
    }

    /**
     * Returns all cost centers of one user
     */
    public ArrayList<String> getCostCenters( String user )
    {
        return userCostCenters.get(user);
    }

    /**
     * Returns all users of one cost center
     */
    public List<String> getUsers( String costCenter )
    {
        return costCenterUsers.get(costCenter);
    }

    @Override
    public String toString()
    {
        return this.userCostCenters.toString();
    }
}
