package com.sap.expenseuploader;

import org.apache.commons.codec.binary.Base64;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import javax.management.relation.RoleNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;

public class IntegrationTest {

    // This currently just uploads data

    @Test
    public void testAgainstTrialInstance()
        throws
        RoleNotFoundException,
        ParseException,
        IOException,
        org.apache.commons.cli.ParseException,
        URISyntaxException,
        java.text.ParseException,
        InvalidFormatException
    {

        String[] args = {
            "--from",               "20150301",
            "--to",                 "20150501",
            "--controlling-area",   "0001",
            "--input_erp",          "system",
            "--output_hcp",
            "--output_cli",
            "--hcp_url",            "https://bmsfin-<your account here>.hanatrial.ondemand.com/core/basic/api/v1",
            "--hcp_user",           "your username here",
            "--hcp_password",       new String(Base64.decodeBase64("your password in base64 here".getBytes())),
            "--hcp_proxy",          "proxy.wdf.sap.corp:8080",
            "--budgets"
        };

        ExpenseUploader.main(args);
    }
}
