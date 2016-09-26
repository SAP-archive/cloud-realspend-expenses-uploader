package com.sap.expenseuploader.config;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.management.relation.RoleNotFoundException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

public class HcpConfig {

    private final Logger logger = LogManager.getLogger(this.getClass());

    // Command line parameters
    private String hcpUrl;
    private String hcpUser;
    private String hcpPass;
    private String proxy; // Null if no proxy

    public HcpConfig(String hcpUrl, String hcpUser, String hcpPass, String proxy) {
        this.hcpUrl = hcpUrl;
        this.hcpUser = hcpUser;
        this.hcpPass = hcpPass;
        this.proxy = proxy;
    }

    public String getHcpUrl()
    {
        if (this.hcpUrl == null || this.hcpUrl.isEmpty()) {
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

    public Request withOptionalProxy(Request request)
    {
        if (this.proxy != null && !this.proxy.isEmpty()) {
            request = request.viaProxy(this.proxy);
        }
        return request;
    }

    /**
     * Fetches a CSRF token from RealSpend and authenticates.
     * @return CSRF token
     * @throws URISyntaxException
     * @throws IOException
     * @throws RoleNotFoundException
     */
    public String getCsrfToken()
            throws URISyntaxException, IOException, RoleNotFoundException
    {
        // Currently the caching is disabled, the token is refetched every time
        return fetchCsrfToken();
    }

    private String fetchCsrfToken() throws IOException, URISyntaxException, RoleNotFoundException {
        URIBuilder uriBuilder = new URIBuilder(getHcpUrl() + "/rest/csrf");
        Request request = Request.Get(uriBuilder.build())
                .addHeader("Authorization", "Basic " + buildAuthString())
                .addHeader("x-csrf-token", "fetch");
        HttpResponse response = withOptionalProxy(request).execute().returnResponse();
        Header responseCsrfHeader = response.getFirstHeader("x-csrf-token");
        if( responseCsrfHeader == null ) {
            throw new RoleNotFoundException("Provided username: \'" + getHcpUser()
                    + "\' is not authorized to perform http requests to HCP or wrong username/password provided.");
        }
        String result = responseCsrfHeader.getValue();
        logger.debug("Fetched CSRF token " + result);
        return result;
    }

    public String buildAuthString()
    {
        return new String(
            Base64.encodeBase64(
                (getHcpUser() + ":" + getHcpPass()).getBytes()
            )
        );
    }

    public static String getBodyFromResponse(HttpResponse r) throws IOException {
        InputStream in = r.getEntity().getContent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        while (true) {
            int nread = in.read(buf, 0, buf.length);
            if (nread <= 0) {
                break;
            }
            baos.write(buf, 0, nread);
        }
        in.close();
        baos.close();
        byte[] bytes = baos.toByteArray();
        return new String(bytes);
    }
}
