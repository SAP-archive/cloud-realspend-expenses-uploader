package com.sap.expenseuploader.expenses.output;

import com.google.gson.*;
import com.sap.expenseuploader.config.ErpExpenseInputConfig;
import com.sap.expenseuploader.config.HcpConfig;
import com.sap.expenseuploader.config.costcenter.CostCenterConfig;
import com.sap.expenseuploader.model.Expense;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.relation.RoleNotFoundException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import static com.sap.expenseuploader.config.HcpConfig.getBodyFromResponse;

/**
 * This is the most common method of output, to upload expenses to the HCP realspend
 * backend.
 */
public class ExpenseHcpOutput implements ExpenseOutput
{
    private final Logger logger = LogManager.getLogger(this.getClass());

    public static final Path REQ_DUMP_FOLDER = Paths.get("requests");
    public static final int MAX_BATCH_SIZE = 1000;

    private HcpConfig hcpConfig;
    private CostCenterConfig costCenterConfig;
    private ErpExpenseInputConfig erpExpenseInputConfig;

    public ExpenseHcpOutput( HcpConfig hcpConfig, CostCenterConfig costCenterConfig,
        ErpExpenseInputConfig erpExpenseInputConfig )
    {
        this.hcpConfig = hcpConfig;
        this.costCenterConfig = costCenterConfig;
        this.erpExpenseInputConfig = erpExpenseInputConfig;
    }

    @Override
    public void putExpenses( List<Expense> expenses )
    {
        logger.info("Writing expenses to HCP at " + this.hcpConfig.getHcpUrl() + "...");

        try {
            // In case the resume function was performed then we don't upload anything new
            boolean hasPerformedResume = maybePerformResume(this.hcpConfig.isResumeSet());

            if (hasPerformedResume) {
                // Don't put expenses, the resume was enough
                return;
            }

            // Used to count batches
            long batchID = 0;

            // Upload
            for( String user : this.costCenterConfig.getUserList() ) {
                List<String> costCenters = this.costCenterConfig.getCostCenters(user);
                List<Expense> userExpenses = new ArrayList<>();
                for( Expense expense : expenses ) {
                    if( expense.isInCostCenter(costCenters) ) {
                        userExpenses.add(expense);
                    }
                }
                if( userExpenses.isEmpty() ) {
                    logger.info("No expenses to put for user " + user);
                    continue;
                }

                int batchCounter = 1;
                while( (batchCounter - 1) * MAX_BATCH_SIZE < userExpenses.size() ) {
                    int fromIndex = (batchCounter - 1) * MAX_BATCH_SIZE;
                    int toIndex = Math.min(batchCounter * MAX_BATCH_SIZE, userExpenses.size());
                    uploadBatchExpenses(userExpenses.subList(fromIndex, toIndex),
                        user,
                        ++batchID);
                    batchCounter++;
                }
            }
        }
        catch( Exception e ) {
            throw new RuntimeException("Failed to post expenses", e);
        }
    }

    /**
     * checks the request folder if it contains any failed requests
     *
     * @param resumeFlagSet the value of resume flag (whether it's set or not)
     * @return returns true if there's no resume required, and false otherwise
     * @throws RoleNotFoundException
     * @throws IOException
     * @throws URISyntaxException
     */
    private boolean maybePerformResume( boolean resumeFlagSet )
        throws RoleNotFoundException, IOException, URISyntaxException
    {
        if( !Files.exists(REQ_DUMP_FOLDER) ) {
            // No resume necessary
            return false;
        }

        String[] failedRequestFilenames = REQ_DUMP_FOLDER.toFile().list(new FilenameFilter()
        {
            @Override
            public boolean accept( File dir, String name )
            {
                if( name.contains("_") && !name.toLowerCase().endsWith("_200.json") ) {
                    return true;
                }
                return false;
            }
        });

        if( failedRequestFilenames.length == 0 ) {
            // In case resume function is set in the cmd options, but it's not required
            if( resumeFlagSet ) {
                logger.info("The previous expense uploading run was successful, no resume is required.");
            }

            // No failed requests in the previous run
            deleteRequestFolder();
            return false;
        }

        // This means we have failed requests from before, we should resume
        if( !resumeFlagSet ) {
            // In case resume is not set but the previous run wasn't successful. -> Force the user to use it
            logger.error(
                    "There are failed requests from a previous run. Either delete them or resume their upload by setting the 'resume' flag.");
            System.exit(1);
        }

        // Resume the previous upload
        logger.info("Resuming the previous upload by retrying failed requests...");
        assertCurrentConfigMatches();
        for( String filename : failedRequestFilenames ) {
            String batchName = filename.substring(0, filename.lastIndexOf("_"));
            Path fullBatchFilepath = Paths.get(REQ_DUMP_FOLDER.toString(), batchName + ".json");
            if( !Files.exists(fullBatchFilepath) ) {
                continue;
            }
            String payloadString = new String(Files.readAllBytes(fullBatchFilepath));
            if( reUploadRequest(payloadString, batchName) ) {
                // deleting the file with failed response in it
                Files.delete(Paths.get(REQ_DUMP_FOLDER.toString(), filename));
                logger.info(
                    "Expenses stored in file " + fullBatchFilepath + " were successfully uploaded.");
            }
        }

        return true;
    }

    private void deleteRequestFolder()
    {
        try {
            if( Files.exists(REQ_DUMP_FOLDER) ) {
                logger.info("Deleting old request folder..");
                Files.walkFileTree(REQ_DUMP_FOLDER, new SimpleFileVisitor<Path>()
                {
                    @Override
                    public FileVisitResult visitFile( Path file, BasicFileAttributes attrs )
                        throws IOException
                    {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory( Path dir, IOException exc )
                        throws IOException
                    {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        }
        catch( Exception e ) {
            logger.error("Failed to delete " + REQ_DUMP_FOLDER, e);
        }
    }

    private void uploadBatchExpenses( List<Expense> expenses, String user, long batchID )
        throws URISyntaxException, IOException, RoleNotFoundException
    {
        // Create JSON payload
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd").create();
        JsonArray expensesAsJson = (JsonArray) gson.toJsonTree(expenses);

        JsonObject payload = new JsonObject();
        payload.add("expenses", expensesAsJson);
        payload.addProperty("user", user);

        // store the current config
        assertRequestsFolderExists();
        dumpConfig();

        // storing the requests as json
        dumpRequest("batch" + batchID, new GsonBuilder().setPrettyPrinting().create().toJson(payload));

        // Upload
        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/expense");
        Request request = Request.Post(uriBuilder.build())
            .addHeader("x-csrf-token", this.hcpConfig.getCsrfToken())
            .bodyString(payload.toString(), ContentType.APPLICATION_JSON);

        logger.info(String.format("Posting %s expenses for user %s...", expenses.size(), user));

        final long start = System.currentTimeMillis();
        HttpResponse response = this.hcpConfig.withOptionalProxy(request).execute().returnResponse();
        final long duration = System.currentTimeMillis() - start;

        dumpResponse("batch" + batchID, response);

        // Check response
        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode == 200 ) {
            logger.info(String.format("Successfully uploaded %s expenses for user %s in %s second(s)",
                expenses.size(),
                user,
                duration / 1000));
        } else {
            logger.error(String.format("Got http code %s while uploading %s expenses for user %s",
                statusCode,
                expenses.size(),
                user));
            logger.error("URL was: " + uriBuilder.build());
            logger.error("Error is: " + getBodyFromResponse(response));
        }
    }

    private boolean reUploadRequest( String payloadString, String batchName )
        throws URISyntaxException, IOException
    {
        // Upload
        logger.info("Re-uploading the request stored in file " + batchName + ".json ...");
        URIBuilder uriBuilder = new URIBuilder(this.hcpConfig.getHcpUrl() + "/rest/expense");
        Request request = Request.Post(uriBuilder.build())
            .addHeader("x-csrf-token", this.hcpConfig.getCsrfToken())
            .bodyString(payloadString, ContentType.APPLICATION_JSON);
        HttpResponse response = this.hcpConfig.withOptionalProxy(request).execute().returnResponse();

        dumpResponse(batchName, response);

        // Check response
        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode == 200 ) {
            return true;
        } else {
            logger.error(String.format("Got http code %s while uploading the expenses from file %s.json",
                statusCode,
                batchName));
            logger.error("URL was: " + uriBuilder.build());
            logger.error("Error is: " + getBodyFromResponse(response));
            logger.error(String.format(
                "Please check the response file \"%s\", and then change the necessary field values in the corresponding payload file \"%s\".",
                REQ_DUMP_FOLDER.toString() + "/" + batchName + "_" + statusCode + ".json",
                REQ_DUMP_FOLDER.toString() + "/" + batchName + ".json"));
            return false;
        }
    }

    private void assertCurrentConfigMatches()
        throws IOException
    {
        final File file = new File(REQ_DUMP_FOLDER.toFile(), "config.txt");
        List<String> oldConfig = Files.readAllLines(file.toPath(), Charset.forName("UTF-8"));
        if( !oldConfig.get(0).equals(this.hcpConfig.toString()) ) {
            logger.error("Your HCP config has changed, the old config was: " + oldConfig.get(0));
            logger.error("Either revert to the previous config or remove the 'requests' folder");
            System.exit(1);
        }
        if( !oldConfig.get(1).equals(this.costCenterConfig.toString()) ) {
            logger.error("Your cost center config has changed, the old config was: " + oldConfig.get(1));
            logger.error("Either revert to the previous config or remove the 'requests' folder");
            System.exit(1);
        }
        if( !oldConfig.get(2).equals(this.erpExpenseInputConfig.toString()) ) {
            logger.error("Your erp expense input config has changed, the old config was: " + oldConfig.get(2));
            logger.error("Either revert to the previous config or remove the 'requests' folder");
            System.exit(1);
        }
    }

    private void assertRequestsFolderExists()
        throws IOException
    {
        try {
            if( !Files.isDirectory(REQ_DUMP_FOLDER) ) {
                Files.createDirectory(REQ_DUMP_FOLDER);
            }
        }
        catch( Exception e ) {
            logger.error("Failed to create the folder " + REQ_DUMP_FOLDER);
            throw e;
        }
    }

    private void dumpConfig()
    {
        final File file = new File(REQ_DUMP_FOLDER.toFile(), "config.txt");
        try( PrintWriter writer = new PrintWriter(file) ) {
            writer.write(this.hcpConfig.toString() + "\n");
            writer.write(this.costCenterConfig.toString() + "\n");
            writer.write(this.erpExpenseInputConfig.toString() + "\n");
        }
        catch( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    private void dumpRequest( final String key, final String s )
    {
        final File file = new File(REQ_DUMP_FOLDER.toFile(), key + ".json");
        try( PrintWriter writer = new PrintWriter(file) ) {
            writer.write(s);
        }
        catch( Exception e ) {
            throw new RuntimeException(e);
        }
    }

    private void dumpResponse( final String key, final HttpResponse response )
    {
        final File file =
            new File(REQ_DUMP_FOLDER.toFile(), key + "_" + response.getStatusLine().getStatusCode() + ".json");
        try( PrintWriter writer = new PrintWriter(file) ) {
            String responseString = getBodyFromResponse(response);

            JsonParser parser = new JsonParser();
            JsonObject json = parser.parse(responseString).getAsJsonObject();
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(json));
        }
        catch( Exception e ) {
            throw new RuntimeException(e);
        }
    }
}
