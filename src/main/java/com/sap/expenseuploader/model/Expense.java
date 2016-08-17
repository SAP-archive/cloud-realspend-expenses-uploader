package com.sap.expenseuploader.model;

import com.google.gson.annotations.SerializedName;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Expense implements Comparable<Expense>
{
    private Date date;
    private String type;
    @SerializedName( "cost-center" )
    private String costCenter;
    private String account;
    private String requester;
    @SerializedName( "internal-order" )
    private String internalOrder;
    private String context;
    private transient String requestID;
    private Double amount;
    private String currency;

    private transient List<String> fields; // Transient to skip serialisation

    public Expense( String... fields )
        throws ParseException
    {
        this(Arrays.asList(fields));
    }

    public Expense( List<String> fields )
        throws ParseException
    {
        this.fields = fields;
        if( fields.size() != 10 ) {
            System.out.println("Called Expense with " + fields.size() + " parameters instead of 10");
        }

        // Fill fixed fields
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        this.date = sdf.parse(fields.get(0));
        this.type = fields.get(1);
        this.costCenter = fields.get(2);
        this.account = fields.get(3);
        this.requester = fields.get(4);
        this.internalOrder = fields.get(5);
        this.context = fields.get(6);
        this.requestID = fields.get(7);
        this.amount = Double.parseDouble(fields.get(8));
        this.currency = fields.get(9);
    }

    public int size()
    {
        return this.fields.size();
    }

    public String get( int i )
    {
        return this.fields.get(i);
    }

    public String getType()
    {
        return type;
    }

    public Date getDate()
    {
        return date;
    }

    public Double getAmount()
    {
        return amount;
    }

    public String getAccount()
    {
        return account;
    }

    public String getContext()
    {
        return context;
    }

    public String getCostCenter()
    {
        return costCenter;
    }

    public String getCurrency()
    {
        return currency;
    }

    public String getInternalOrder()
    {
        return internalOrder;
    }

    public String getRequester()
    {
        return requester;
    }

    public String getRequestID()
    {
        return requestID;
    }

    public boolean isInCostCenter( List<String> costCenters )
    {
        for( String cc : costCenters ) {
            if( this.getCostCenter().equalsIgnoreCase(cc) ) {
                return true;
            }
        }
        return false;
    }

    private int nullSafeCompare( Comparable o1, Object o2 )
    {
        if( o1 == null && o2 == null ) {
            return 0;
        }
        if( o1 == null ) {
            return -1;
        }
        if( o1 instanceof String && o2 instanceof String ) {
            // Case-insensitive string comparison
            return ((String) o1).compareToIgnoreCase((String) o2);
        }
        return o1.compareTo(o2);
    }

    @Override
    public int compareTo( Expense other )
    {
        int result;
        result = nullSafeCompare(this.date, other.getDate());
        if( result != 0 ) {
            return result;
        }
        result = nullSafeCompare(this.type, other.getType());
        if( result != 0 ) {
            return result;
        }
        result = nullSafeCompare(this.amount, other.getAmount());
        if( result != 0 ) {
            return result;
        }
        result = nullSafeCompare(this.currency, other.getCurrency());
        if( result != 0 ) {
            return result;
        }
        return 0;
    }

    @Override
    public boolean equals( Object other )
    {
        if( !(other instanceof Expense) ) {
            return false;
        }
        return this.compareTo((Expense) other) == 0;
    }

    @Override
    public String toString()
    {
        return this.fields.toString();
    }
}
