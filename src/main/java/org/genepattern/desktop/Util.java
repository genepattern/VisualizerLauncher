package org.genepattern.desktop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Created by nazaire on 4/6/16.
 */
public class Util {
    final static private Logger log = LogManager.getLogger(Util.class);
    
    public static boolean isNullOrEmpty(final String str) {
        return (str==null || str.length()==0);
    }

    public static String nullToEmpty(final String str) {
        if (str==null) {
            return "";
        }
        else {
            return str;
        }
    }

    protected static String initBasicAuthHeader(final String username, final String password) {
        if (username==null || username.length()==0) {
            throw new IllegalArgumentException("Missing required parameter: username");
        }
        final String user=username+":"+nullToEmpty(password);
        byte[] authEncBytes = Base64.encodeBase64(user.getBytes());
        return "Basic " + new String(authEncBytes);
    }

    protected static String doGetRequest(final String basicAuthHeader, final String fromUrl) throws IOException
    {
        final CloseableHttpClient httpClient = HttpClientBuilder.create().build();
        try {
            final HttpGet httpget = new HttpGet(fromUrl);
            if (!isNullOrEmpty(basicAuthHeader)) {
                httpget.setHeader("Authorization", basicAuthHeader);
            }
            log.debug("Executing request " + httpget.getRequestLine());

            // Create a custom response handler
            final ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException {
                    final int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        final HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } 
                    else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }
            };
            return httpClient.execute(httpget, responseHandler);
        }
        finally {
            httpClient.close();
        }
    }

    /**
     * Download a URL to a local file and return a File object for it.
     *
     * @param url, The url to download.
     * @param dir, The directory to download the URL to.
     * @param filename, The filename to download the URL to.
     */
    public static File downloadFile(String authString, URL url, File dir, String filename) throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            URLConnection conn = url.openConnection();
            conn.setRequestProperty("Authorization", authString);

            is = conn.getInputStream();
            dir.mkdirs();
            file = new File(dir, filename);
            fos = new FileOutputStream(file);
            byte[] buf = new byte[100000];
            int j;
            while ((j = is.read(buf, 0, buf.length)) != -1) {
                fos.write(buf, 0, j);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException e) {
                }
            }
        }
        return file;
    }
}
