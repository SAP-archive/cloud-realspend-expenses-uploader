package com.sap.expenseuploader.config;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URISyntaxException;

public class HcpConfig
{
    private static final Logger logger = LogManager.getLogger(HcpConfig.class);

    // Command line parameters
    private String hcpUrl;
    private String hcpUser;
    private String hcpPass;
    private String proxy; // Can be null
    private boolean resume;

    // CSRF token
    private String csrfToken;

    public HcpConfig( String hcpUrl, String hcpUser, String hcpPass, String proxy, boolean resume )
    {
        this.hcpUrl = hcpUrl;
        this.hcpUser = hcpUser;
        this.hcpPass = hcpPass;
        this.proxy = proxy;
        this.resume = resume;
    }

    public String getHcpUrl()
    {
        if( this.hcpUrl == null || this.hcpUrl.isEmpty() ) {
            logger.error("No HCP URL provided!");
        }
        return this.hcpUrl;
    }

    public String getHcpUser()
    {
        if( this.hcpUser == null ) {
            // If this was not provided, show prompt
            if( System.console() == null ) {
                logger.error("Unable to prompt for username!");
                return null;
            }
            this.hcpUser = System.console().readLine("HCP Username: ");
        }
        return this.hcpUser;
    }

    public String getHcpPass()
    {
        if( this.hcpPass == null ) {
            // If this was not provided, show prompt
            if( System.console() == null ) {
                logger.error("Unable to prompt for password!");
                return null;
            }
            this.hcpPass = new String(System.console().readPassword("HCP Password: "));
        }
        return this.hcpPass;
    }

    public boolean isResumeSet()
    {
        return resume;
    }

    public Request withOptionalProxy( Request request )
    {
        if( this.proxy != null && !this.proxy.isEmpty() ) {
            request = request.viaProxy(this.proxy);
        }
        return request;
    }

    /**
     * Fetches a CSRF token from RealSpend and authenticates.
     */
    public String getCsrfToken()
        throws IOException, URISyntaxException
    {
        // Currently the caching is disabled, the token is refetched every time
        return fetchCsrfToken();
    }

    private String fetchCsrfToken()
        throws IOException, URISyntaxException
    {
        URIBuilder uriBuilder = new URIBuilder(getHcpUrl() + "/rest/csrf");
        Request request = Request.Get(uriBuilder.build())
            .addHeader("Authorization", "Basic " + buildAuthString())
            .addHeader("x-csrf-token", "fetch");
        HttpResponse response = withOptionalProxy(request).execute().returnResponse();
        Header responseCsrfHeader = response.getFirstHeader("x-csrf-token");
        int statusCode = response.getStatusLine().getStatusCode();
        if( statusCode != 200 ) {
            logger.error(String.format("Got http code %s while fetching CSRF token", statusCode));
            logger.error("URL was: " + uriBuilder.build());
            logger.error("Error is: " + getBodyFromResponse(response));
        }
        if( responseCsrfHeader == null ) {
            throw new RuntimeException("Failed to fetch CSRF token.");
        }
        String result = responseCsrfHeader.getValue();
        logger.debug("Fetched CSRF token " + result);
        return result;
    }

    public String buildAuthString()
    {
        return new String(Base64.encodeBase64((getHcpUser() + ":" + getHcpPass()).getBytes()));
    }

    public static String getBodyFromResponse( HttpResponse r )
        throws IOException
    {
        return EntityUtils.toString(r.getEntity());
    }

    @Override
    public String toString()
    {
        return String.format("url=%s, user=%s", this.hcpUrl, this.hcpUser);
    }
}
