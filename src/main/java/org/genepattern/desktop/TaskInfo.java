package org.genepattern.desktop;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Helper class for downloading the module support files.
 * @author pcarr
 *
 */
public class TaskInfo {
    private static final Logger log = LogManager.getLogger(TaskInfo.class);

    protected static JSONObject getTaskJson(final GpServerInfo info, final String taskLsid) 
    throws IOException {
        final String getTaskRESTCall = 
                info.getGpServer() + VisualizerLauncher.REST_API_TASK_PATH  + "/" + taskLsid + "?includeSupportFiles=true";
        final String response =
                Util.doGetRequest(info.getBasicAuthHeader(), getTaskRESTCall);
        return new JSONObject(response);
    }
    
    public static TaskInfo createFromLsid(final GpServerInfo info, final String taskLsid) throws Exception {
        final JSONObject taskJson = getTaskJson(info, taskLsid);
        //check if this is a java visualizer
        if (!taskJson.has("taskType")) {
            throw new Exception("No taskType property found. The taskType must be set to Visualizer");
        }
        final String taskType = taskJson.getString("taskType");
        if (!taskType.toLowerCase().equals("visualizer")) {
            throw new Exception("Unexpected taskType: " + taskType + ". Expecting the taskType to be \'Visualizer\'.");
        }
        
        final TaskInfo taskInfo=new TaskInfo();
        taskInfo.lsid=taskLsid;
        taskInfo.name=taskJson.getString("name");
        taskInfo.version=taskJson.getString("version");
        taskInfo.commandLine = taskJson.getString("command_line");
        if (Util.isNullOrEmpty(taskInfo.commandLine)) {
            throw new Exception("Missing required 'command_line' value for lsid=" + taskLsid);
        }
        if (!taskJson.has("supportFiles")) {
            throw new Exception("No supportFiles property found. Please check if this is GenePattern version 3.9.8 or greater.");
        }
        final JSONArray supportFiles = taskJson.getJSONArray("supportFiles");
        if (supportFiles == null || supportFiles.length() == 0) {
            throw new Exception("No support files found for task with LSID: " + taskLsid);
        }
        for (int i=0; i<supportFiles.length(); ++i) {
            taskInfo.supportFileUrls.add(info.getGpServer() + supportFiles.getString(i));
        }
        
        // <app.dir>/taskLib/<task.name>_v<task.version>/
        File appDir=FileUtil.getAppDir();
        taskInfo.libdir=new File(appDir, "taskLib/"+taskInfo.name+"_v"+taskInfo.version);
        return taskInfo;
    }
    
    private String name;
    private String lsid;
    private String version;
    private File libdir;
    private String commandLine;
    private List<String> supportFileUrls=new ArrayList<String>();
    
    public File getLibdir() {
        return libdir;
    }
    
    public String getCommandLine() {
        return commandLine;
    }

    protected boolean checkCache=true;
    
    protected String extractFilename(final String supportFileUrl) {
        final int slashIndex = supportFileUrl.lastIndexOf('=');
        final String filename =  supportFileUrl.substring(slashIndex + 1);
        return filename;
    }

    public void downloadSupportFiles(final GpServerInfo info) throws Exception {
        if (supportFileUrls==null) {
            log.error("supportFileUrls not set");
            return;
        }
        if (supportFileUrls.size()==0) {
            log.warn("supportFileUrls.size==0");
            return;
        }
        if (log.isInfoEnabled()) {
            final String fromUrl=supportFileUrls.get(0);
            final String filename=extractFilename(fromUrl);
            final String baseUrl = fromUrl.substring(0, fromUrl.length() - filename.length());
            log.info("     from baseUrl: "+baseUrl);
            log.info("     to libdir:    "+libdir);
        }

        for(final String fromUrl : supportFileUrls) {
            try {
                final String filename = extractFilename(fromUrl);
                final File toFile=new File(libdir, filename);
                if (checkCache && toFile.exists()) {
                    log.info("     (cached) '"+filename+"'");
                }
                else {
                    log.info("     downloading '"+filename+"' ...");
                    FileUtil.downloadFile(info.getBasicAuthHeader(), new URL(fromUrl), toFile);
                }
            }
            catch (Throwable t) {
                throw new Exception("Error downloading support file: '"+fromUrl+"'"+t.getMessage());
            }
        }
    }

}
