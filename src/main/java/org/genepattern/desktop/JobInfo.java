package org.genepattern.desktop;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JobInfo {
    private static final Logger log = LogManager.getLogger(JobInfo.class);
    public static final String REST_API_JOB_PATH  = "/rest/v1/jobs";
    
    public static JobInfo createFromJobId(final GpServerInfo info) throws Exception {
        final JobInfo jobInfo=new JobInfo();
        jobInfo.jobId=info.getJobNumber();
        // <app.dir>/jobs/<jobid>
        final File appDir=FileUtil.getAppDir();
        jobInfo.jobdir = new File(appDir, "jobs/" + info.getJobNumber());
        jobInfo.taskLsid=JobInfo.retrieveJobDetails(info.getBasicAuthHeader(), info.getGpServer(), info.getJobNumber());
        jobInfo.retrieveInputFileDetails(info);
        return jobInfo;
    }
    
    // GET /rest/v1/jobs/{jobId}
    protected static String retrieveJobDetails(final String basicAuthHeader, final String gpServer, final String jobId) 
    throws Exception, JSONException {
        final String response = Util.doGetRequest(basicAuthHeader, 
            gpServer + REST_API_JOB_PATH + "/" + jobId);
        log.trace(response);
        final JSONObject root = new JSONObject(response);
        final String taskLsid = root.getString("taskLsid");
        if(taskLsid == null || taskLsid.length() == 0) {
            throw new Exception("taskLsid not found");
        }
        return taskLsid;
    }
    
    protected JobInfo() {
    }
    
    private String jobId;
    private String taskLsid;
    protected File jobdir;
    private String[] commandLineLocal;
    protected boolean checkCache=true;

    /** map of url->local_path */
    private Map<String, String> inputFiles = new LinkedHashMap<String,String>();
    
    public String getTaskLsid() {
        return taskLsid;
    }
    
    public String[] getCmdLineLocal() {
        return commandLineLocal;
    }
    
    // GET /rest/v1/jobs/{jobId}/visualizerInputFiles
    public void retrieveInputFileDetails(final GpServerInfo info) throws Exception {
        final String inputFilesJson = Util.doGetRequest(
                info.getBasicAuthHeader(), 
                info.getGpServer() + REST_API_JOB_PATH  + "/" + jobId + "/visualizerInputFiles");
        if (log.isTraceEnabled()) {
            log.trace(inputFilesJson);
        }

        final JSONObject inputFilesJsonObj=new JSONObject(inputFilesJson);
        final JSONArray inputFilesArr=inputFilesJsonObj.getJSONArray("inputFiles");
        for(int i=0;i<inputFilesArr.length();i++) {
            addInputFile(info, inputFilesArr.getString(i));
        }
    }

    protected void addInputFile(final GpServerInfo info, final String inputFile) {
        final String inputFileUrlStr=initInputFileUrlStr(info, inputFile);
        final String filenameWithExtension=extractFilenameFromUrl(inputFileUrlStr);
        this.inputFiles.put(inputFileUrlStr, filenameWithExtension);
    }

    protected static String initInputFileUrlStr(final GpServerInfo info, final String inputFile) {
        if (inputFile.startsWith("<GenePatternURL>/")) {
            return inputFile.replaceFirst("<GenePatternURL>/", info.getGpServer()+"/");
        }
        else if (inputFile.startsWith("<GenePatternURL>")) {
            return inputFile.replaceFirst("<GenePatternURL>", info.getGpServer()+"/");
        }
        else if (inputFile.startsWith("/gp/")) {
            // e.g. gpServer=http://127.0.0.1:8080/gp
            return inputFile.replaceFirst("/gp", info.getGpServer());
        }
        else {
            return inputFile;
        }
    }

    protected static String extractFilenameFromUrl(final String fromUrl) {
        String path;
        try {
            path=new URL(fromUrl).toURI().getPath();
        }
        catch (Throwable t) {
            log.error("Error converting url to file path, fromUrl='"+fromUrl+"'", t);
            path=fromUrl;
        }
        final int idx = path.lastIndexOf('/');
        final String filename = path.substring(idx + 1);
        return filename;
    }

    public void downloadInputFiles(final GpServerInfo info) throws Exception {
        if (inputFiles==null) {
            log.error("inputFiles not set");
            return;
        }
        if (inputFiles.size()==0) {
            log.warn("inputFiles.size==0");
            return;
        }
        if (log.isInfoEnabled()) {
            log.info("     to jobdir: "+jobdir);
        }

        for(Entry<String,String> entry : inputFiles.entrySet()) {
            final String fromUrl=entry.getKey();
            final String filename=entry.getValue();
            try {
                final File toFile=new File(jobdir, filename);
                if (checkCache && toFile.exists()) {
                    log.info("     (cached) '"+filename+"'");
                }
                else {
                    log.info("     downloading '"+filename+"' ...");
                    FileUtil.downloadFile(info.getBasicAuthHeader(), new URL(fromUrl), toFile);                
                }
            }
            catch (Throwable t) {
                throw new Exception("Error downloading input file: '"+fromUrl+"'"+t.getMessage());
            }
        }
    }
    
    /** helper method: surround each space-delimited token in double quotes */
    protected static String wrapTokensInQuotes(final String cmdLineIn) {
        String cmdLine="";
        String[] args=cmdLineIn.split(" ");
        for(final String argIn : args) {
            if (!Util.isNullOrEmpty(argIn)) {
                // wrap everything in double quotes
                final String arg = "\"" + argIn + "\" ";
                cmdLine=cmdLine+arg;
            }
        }
        // strip trailing " "
        cmdLine=cmdLine.substring(0, cmdLine.length()-1);
        return cmdLine;
    }
    
    protected static String wrapInQuotes(final String arg) {
        return "\"" + arg + "\"";
    }

    public void prepareCommandLineStep(final GpServerInfo info, final File libdir, final String commandLine) throws IOException {
        if (commandLine==null) {
            throw new IllegalArgumentException("commandLine==null");
        }
        if (inputFiles==null) {
            throw new IllegalArgumentException("inputFiles==null");
        }
        //wrap args in double quotes
        String cmdLine = wrapTokensInQuotes(commandLine);
        //substitute <libdir> with the downloadLocation (aka local libdir)
        cmdLine = cmdLine.replace("<libdir>", libdir.getAbsolutePath() + "/");
        //substitute <path.separator> on local VM, not necessarily the same as server 
        cmdLine = cmdLine.replace("<path.separator>", File.pathSeparator);
        //substitute <java> 
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        //add .exe extension if this is Windows
        java += (System.getProperty("os.name").startsWith("Windows") ? ".exe" : "");
        cmdLine = cmdLine.replace("<java>", java);

        //get the substituted commandline from the serverField
        final String getTaskRESTCall = 
                info.getGpServer() + JobInfo.REST_API_JOB_PATH  + "/" + info.getJobNumber() + "/visualizerCmdLine?commandline=" + VisualizerLauncher.encodeURIcomponent(cmdLine);
        final String response = Util.doGetRequest(info.getBasicAuthHeader(), getTaskRESTCall);

        final JSONTokener tokener = new JSONTokener(response);
        final JSONObject root = new JSONObject(tokener);
        final JSONArray cmdLineArr = root.getJSONArray("commandline");
        log.debug("commandLine (from server): " + cmdLineArr);

        this.commandLineLocal=initCmdLineLocal(info, cmdLineArr);
        log.debug("commandLine (local): " + Arrays.asList(commandLineLocal));
    }

    /** convert from JSONArray to String[]. */
    protected static String[] asStringArray(final JSONArray jsonArr) {
        final int K = jsonArr.length();
        final String[] arr = new String[K];
        for(int i=0; i<K; i++) {
            arr[i]=jsonArr.getString(i);
        }
        return arr;
    }

    protected String[] initCmdLineLocal(final GpServerInfo info, final JSONArray commandlineJson) {
        final String[] cmdLineLocal = asStringArray( commandlineJson );
        substituteLocalFilePaths(info, cmdLineLocal);
        return cmdLineLocal;
    }

    protected String[] substituteLocalFilePaths(final GpServerInfo info, final String[] cmdLineLocal) {
        for(int i=0;i< cmdLineLocal.length; i++) {
            cmdLineLocal[i] = substituteLocalFilePath(info, cmdLineLocal[i]);
        }
        return cmdLineLocal;
    }

    protected String substituteLocalFilePath(final GpServerInfo info, String argValue) {
        if (argValue.startsWith("/gp/")) {
            // e.g. gpServer=http://127.0.0.1:8080/gp
            argValue=argValue.replaceFirst("/gp", info.getGpServer());
        }
        if (inputFiles.containsKey(argValue)) {
            if (jobdir != null) {
                argValue = jobdir.getAbsolutePath() + "/" + inputFiles.get(argValue);
            }
        }
        else {
            argValue = substituteLocalFilePath2(argValue);
        }
        return argValue;
    }

    protected String substituteLocalFilePath2(String arg) {
        for(final Entry<String,String> entry : inputFiles.entrySet()) {
            final String remoteUrl=entry.getKey();
            final String localPath= jobdir.getAbsolutePath() + "/" + entry.getValue();
            arg=arg.replaceAll(remoteUrl, localPath);
        }
        return arg;
    }

}
