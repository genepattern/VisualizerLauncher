package org.genepattern.desktop;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by nazaire on 4/6/16.
 */
public class Util {
    private static final Logger log = LogManager.getLogger(Util.class);
    
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
            if (log.isDebugEnabled()) {
                log.debug("Executing request " + httpget.getRequestLine());
            }

            // Create a custom response handler
            final ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
                @Override
                public String handleResponse(final HttpResponse response) throws ClientProtocolException, IOException { 
                    final int status = response.getStatusLine().getStatusCode();
                    if (log.isDebugEnabled()) {
                        log.debug("response: "+response.getStatusLine());
                    }
                    if (status >= 200 && status < 300) {
                        final HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } 
                    else {
                        throw new ClientProtocolException(response.getStatusLine()+", "+fromUrl);
                    }
                }
            };
            return httpClient.execute(httpget, responseHandler);
        }
        finally {
            httpClient.close();
        }
    }

    protected static String retrieveJobDetails(final String basicAuthHeader, final String gpServer, final String jobId) 
    throws Exception, JSONException {
        final String response = Util.doGetRequest(basicAuthHeader, 
            gpServer + VisualizerLauncher.REST_API_JOB_PATH + "/" + jobId);
        log.trace(response);
        final JSONObject root = new JSONObject(response);
        final String taskLsid = root.getString("taskLsid");
        if(taskLsid == null || taskLsid.length() == 0) {
            throw new Exception("taskLsid not found");
        }
        return taskLsid;
    }

    /**
     * Download a URL to a local file and return a File object for it.
     *
     * @param url, The url to download.
     * @param dir, The directory to download the URL to.
     * @param filename, The filename to download the URL to.
     */
    public static File downloadFile(String authString, URL fromUrl, File toDir, String filename) throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        File file = null;
        try {
            URLConnection conn = fromUrl.openConnection();
            conn.setRequestProperty("Authorization", authString);

            is = conn.getInputStream();
            toDir.mkdirs();
            file = new File(toDir, filename);
            fos = new FileOutputStream(file);
            byte[] buf = new byte[100000];
            int j;
            while ((j = is.read(buf, 0, buf.length)) != -1) {
                fos.write(buf, 0, j);
            }
        }
        catch (IOException e) {
            log.error(e);
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
    
    /** create thread to read from a process output or error stream */
    protected static final Thread copyStream(final InputStream is, final PrintStream out) {
        Thread copyThread = new Thread(new Runnable() {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
                try {
                    while ((line = in.readLine()) != null) {
                        out.println(line);
                    }
                } 
                catch (IOException ioe) {
                    log.error("Error reading from process stream.", ioe);
                }
            }
        });
        copyThread.setDaemon(true);
        copyThread.start();
        return copyThread;
    }

    public static void runCommand(final String[] command) { 
        Thread t = new Thread() {
            public void run() {
                Process process = null;
                try {
                    ProcessBuilder probuilder = new ProcessBuilder(command);
                    process = probuilder.start();
                }
                catch (IOException e) {
                    log.error("Error starting visualizer, command="+command,  e);
                    return;
                }

                // drain the output and error streams
                copyStream(process.getInputStream(), System.out);
                copyStream(process.getErrorStream(), System.err);

                try {
                    @SuppressWarnings("unused")
                    int exitValue = process.waitFor();
                } 
                catch (InterruptedException e) {
                    log.error(e);
                }
            }
        };
        t.start();
    }

}
