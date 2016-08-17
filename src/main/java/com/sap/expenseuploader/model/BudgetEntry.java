package com.sap.expenseuploader.model;

import org.json.simple.JSONObject;

public class BudgetEntry
{
    public double amount;
    public String currency;

    /**
     * the reporting period
     */
    public long period;

    public BudgetEntry( double amount, String currency, long period )
    {
        this.amount = amount;
        this.currency = currency;
        this.period = period;
    }

    public BudgetEntry( JSONObject obj )
    {
        this.amount = Double.parseDouble(obj.get("amount").toString());
        this.currency = (String) obj.get("currency");
        this.period = (long) obj.get("period");
    }

    @Override
    public String toString()
    {
        return String.format("[%s, %s, %s]", amount, currency, period);
    }
}
