package com.sap.expenseuploader;

import com.sap.conn.jco.*;
import com.sap.expenseuploader.config.ErpExpenseInputConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a library of helper functions which can be used by other classes
 */
public class Helper
{
    private static final String COST_CENTER_BAPI_NAME = "BAPI_COSTCENTER_GETLIST";
    private static final String COST_CENTER_TABLE_NAME = "COSTCENTER_LIST";

    public static String stripLeadingZeros( String str )
    {
        return str.replaceFirst("^0+(?!$)", "");
    }

    public static Set<String> getErpCostCenters( ErpExpenseInputConfig erpExpenseInputConfig )
        throws JCoException
    {
        Logger logger = LogManager.getLogger(Helper.class);

        logger.info("Reading cost centers from the ERP ...");

        JCoDestination destination = erpExpenseInputConfig.getJcoDestination();
        JCoRepository repository = destination.getRepository();
        JCoContext.begin(destination);
        JCoFunction bapiCostCenterList = repository.getFunctionTemplate(COST_CENTER_BAPI_NAME).getFunction();

        bapiCostCenterList.getImportParameterList()
            .setValue("CONTROLLINGAREA", erpExpenseInputConfig.getControllingArea());

        // Execute BAPI
        bapiCostCenterList.execute(destination);

        // Read returned table
        JCoTable costCenterTable = bapiCostCenterList.getTableParameterList().getTable(COST_CENTER_TABLE_NAME);
        logger.debug(String.format("Found %s cost centers for controlling area %s in the ERP",
            costCenterTable.getNumRows(),
            erpExpenseInputConfig.getControllingArea()));

        Set<String> result = new HashSet<>();
        for( int i = 0; i < costCenterTable.getNumRows(); i++ ) {
            costCenterTable.setRow(i);
            String costCenter = costCenterTable.getString("COSTCENTER");
            result.add(stripLeadingZeros(costCenter));
        }
        return result;
    }
}
