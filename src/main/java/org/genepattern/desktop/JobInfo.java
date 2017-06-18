package org.genepattern.desktop;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

public class JobInfo {
    private static final Logger log = LogManager.getLogger(JobInfo.class);
    public static final String REST_API_JOB_PATH  = "/rest/v1/jobs";
    
    /**
     * Get the local directory for job input files,
     *   default: <appDir>/jobs/<jobId>
     * 
     * @param jobId
     * @return
     */
    public static File initLocalJobDir(final String jobId) {
        final File appDir=AppDirUtil.getAppDir();
        return initLocalJobDir(appDir, jobId);
    }

    protected static File initLocalJobDir(final File appDir, final String jobId) {
        final File jobDir=new File(appDir, "jobs/" + jobId);
        return jobDir;
    }

    public static JobInfo createFromJobId(final GpServerInfo info) throws Exception {
        final File jobDir = initLocalJobDir(info.getJobNumber());
        final JobInfo jobInfo=new JobInfo(info.getJobNumber(), jobDir);
        jobInfo.taskLsid=JobInfo.retrieveJobDetails(info.getBasicAuthHeader(), info.getGpServer(), info.getJobNumber());
        jobInfo.retrieveInputFileDetails(info);
        return jobInfo;
    }

    // GET /rest/v1/jobs/{jobId}
    protected static String retrieveJobDetails(final String basicAuthHeader, final String gpServer, final String jobId) 
    throws Exception, JSONException {
        final String response = Util.doGetRequest(log, basicAuthHeader, 
            gpServer + REST_API_JOB_PATH + "/" + jobId);
        log.trace(response);
        final JSONObject root = new JSONObject(response);
        final String taskLsid = root.getString("taskLsid");
        if(taskLsid == null || taskLsid.length() == 0) {
            throw new Exception("taskLsid not found");
        }
        return taskLsid;
    }
    
    protected JobInfo(final String jobId, final File jobdir) {
        this.jobId=jobId;
        this.jobdir=jobdir;
    }
    
    private final String jobId;
    private String taskLsid;
    private final File jobdir;
    private String[] commandLineLocal;
    protected boolean checkCache=true;

    private List<InputFileInfo> inputFiles = new ArrayList<InputFileInfo>();

    public String getTaskLsid() {
        return taskLsid;
    }
    
    public List<InputFileInfo> getInputFiles() {
        return Collections.unmodifiableList(inputFiles);
    }

    public String[] getCmdLineLocal() {
        return commandLineLocal;
    }
    
    // GET /rest/v1/jobs/{jobId}/visualizerInputFiles
    public void retrieveInputFileDetails(final GpServerInfo info) throws Exception {
        final String inputFilesJson = Util.doGetRequest(log,
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
        if (Util.isNullOrEmpty(inputFile)) {
            log.info("Skipping inputFile='"+inputFile+"'");
            return;
        }
        final InputFileInfo inputFileInfo=new InputFileInfo(info, inputFile);
        if (inputFileInfo != null) {
            inputFiles.add(inputFileInfo);
        }
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

        for(final InputFileInfo inputFile : inputFiles) {
            final String fromUrl=inputFile.getUrl();
            final String filename=inputFile.getFilename();
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

    public void prepareCommandLineStep(final GpServerInfo info, final File libdir, final String commandLine) throws IOException {
        if (commandLine==null) {
            throw new IllegalArgumentException("commandLine==null");
        }
        if (inputFiles==null) {
            throw new IllegalArgumentException("inputFiles==null");
        }
        final String cmdLine = preprocessCmdLine(libdir, commandLine);

        final String getTaskRESTCall = 
                info.getGpServer() + JobInfo.REST_API_JOB_PATH  + "/" + info.getJobNumber() + "/visualizerCmdLine?commandline=" + Util.encodeURIcomponent(cmdLine);
        final String response = Util.doGetRequest(log, info.getBasicAuthHeader(), getTaskRESTCall);

        final JSONTokener tokener = new JSONTokener(response);
        final JSONObject root = new JSONObject(tokener);
        final JSONArray cmdLineArr = root.getJSONArray("commandline");
        log.debug("commandLine (from server): " + cmdLineArr);

        this.commandLineLocal=initCmdLineLocal(info, cmdLineArr);
        log.debug("commandLine (local): " + Arrays.asList(commandLineLocal));
    }

    // do local substitutions 
    protected String preprocessCmdLine(final File libdir, final String commandLine) {
        //wrap args in double quotes
        String cmdLine = wrapTokensInQuotes(commandLine);
        //substitute <libdir> with the downloadLocation (aka local libdir)
        cmdLine = cmdLine.replace("<libdir>", libdir.getAbsolutePath() + "/");
        //substitute <path.separator> on local VM, not necessarily the same as server 
        cmdLine = cmdLine.replace("<path.separator>", File.pathSeparator);
        //substitute <java> 
        final String java = getSystemJavaCmd();
        cmdLine = cmdLine.replace("<java>", java);
        return cmdLine;
    }

    /**
     * Get the local path to the java executable,
     * for the '<java>' command line substitution. 
     */
    protected static String getSystemJavaCmd() { 
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        //add .exe extension if this is Windows
        java += (System.getProperty("os.name").startsWith("Windows") ? ".exe" : "");
        return java;
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
        return substituteLocalFilePaths(info, cmdLineLocal);
    }

    /**
     * Process each commmand line arg, replace all input file urls with local file paths.
     * 
     * @param info
     * @param cmdLineLocal
     * @return
     */
    protected String[] substituteLocalFilePaths(final GpServerInfo info, final String[] cmdLineLocal) {
        // for each cmd line arg ...
        for(int i=0; i< cmdLineLocal.length; i++) {
            String cmdLineArg=cmdLineLocal[i];
            // for each input file ...
            for(final InputFileInfo inputFile : inputFiles) {
                cmdLineArg = inputFile.substituteLocalPath(cmdLineArg, jobdir);
            }
            cmdLineLocal[i] = cmdLineArg;
        }
        return cmdLineLocal;
    }

}
