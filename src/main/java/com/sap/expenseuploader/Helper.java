package com.sap.expenseuploader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a library of helper functions which can be used by other classes
 */
public class Helper
{
    private static final Logger logger = LogManager.getLogger(Helper.class);

    public static String stripLeadingZeros( String str )
    {
        return str.replaceFirst("^0+(?!$)", "");
    }
}
