package com.sap.expenseuploader.model;

/**
 * A pair of date and currency, used mainly when retrieving the expenses from the ERP side
 */
public class ControllingDocumentData
{
    private String documentDate = "";
    private String documentCurrency = "";

    public ControllingDocumentData( String documentDate, String documentCurrency )
    {
        this.documentCurrency = documentCurrency;
        this.documentDate = documentDate;
    }

    public String getDocumentCurrency()
    {
        return documentCurrency;
    }

    public String getDocumentDate()
    {
        return documentDate;
    }

    @Override
    public String toString()
    {
        return "Document Date = " + this.documentDate + " -- Document Currency = " + this.documentCurrency;
    }
}
