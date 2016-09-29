package com.sap.expenseuploader.config;

import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoDestinationManager;
import com.sap.conn.jco.JCoException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class ErpExpenseInputConfig
{
    private final Logger logger = LogManager.getLogger(this.getClass());

    // Command line parameters for expenses
    private String systemName;
    private String controllingArea;
    private String fromTime;
    private String toTime;
    private String period;

    public ErpExpenseInputConfig( String systemName, String controllingArea, String fromTime, String toTime,
        String period )
        throws IOException, ParseException
    {
        this.systemName = systemName;
        this.controllingArea = controllingArea;
        this.fromTime = fromTime;
        this.toTime = toTime;
        this.period = period;
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
            logger.info("Setting to-time to " + this.toTime);
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

    public JCoDestination getJcoDestination()
        throws JCoException
    {
        return JCoDestinationManager.getDestination(this.systemName);
    }
}
