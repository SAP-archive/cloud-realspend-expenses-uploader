package com.sap.expenseuploader;

import com.sap.expenseuploader.config.costcenter.CostCenterConfig;
import com.sap.expenseuploader.config.costcenter.ExcelCostCenterConfig;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExcelCostCenterConfigTest {

    @Test
    public void testWorkingCostCenterConfig() throws IOException, InvalidFormatException
    {
        CostCenterConfig config = new ExcelCostCenterConfig("src/test/resources/config/working.xlsx");
        System.out.println(config.toString());

        assertEquals("[1104340, 1104341, 1104342]", config.getCostCenterList().toString());
        assertEquals("[S12345670, S12345678, S12345679]", config.getCostCenterUserList().toString());
        assertTrue(config.getCostCenters("S12345670").contains("1104341"));
        assertTrue(config.getCostCenters("S12345678").contains("1104340"));
        assertTrue(config.getCostCenters("S12345678").contains("1104342"));
        assertTrue(config.getCostCenters("S12345679").contains("1104340"));
        assertEquals(1, config.getCostCenters("S12345670").size());
        assertEquals(2, config.getCostCenters("S12345678").size());
        assertEquals(1, config.getCostCenters("S12345679").size());
        assertTrue(config.getUsers("1104340").contains("S12345678"));
        assertTrue(config.getUsers("1104340").contains("S12345679"));
        assertTrue(config.getUsers("1104341").contains("S12345670"));
        assertTrue(config.getUsers("1104342").contains("S12345678"));
        assertEquals(2, config.getUsers("1104340").size());
        assertEquals(1, config.getUsers("1104341").size());
        assertEquals(1, config.getUsers("1104342").size());
    }
}
