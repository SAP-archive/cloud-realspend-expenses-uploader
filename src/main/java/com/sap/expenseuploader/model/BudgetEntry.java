package com.sap.expenseuploader.model;

import org.json.simple.JSONObject;

public class BudgetEntry
{
    public double amount;
    public String currency;

    /**
     * The reporting period
     */
    public long year;

    public BudgetEntry( double amount, String currency, long year )
    {
        this.amount = amount;
        this.currency = currency;
        this.year = year;
    }

    public BudgetEntry( JSONObject obj )
    {
        this.amount = Double.parseDouble(obj.get("amount").toString());
        this.currency = (String) obj.get("currency");
        this.year = (long) obj.get("year");
    }

    @Override
    public String toString()
    {
        return String.format("[%s, %s, %s]", amount, currency, year);
    }

    @Override
    public boolean equals( Object other )
    {
        if( !(other instanceof BudgetEntry) ) {
            return false;
        }
        BudgetEntry otherEntry = (BudgetEntry) other;
        if( this.amount != otherEntry.amount ) {
            return false;
        }
        if( !this.currency.equals(otherEntry.currency) ) {
            return false;
        }
        if( this.year != otherEntry.year ) {
            return false;
        }
        return true;
    }
}
