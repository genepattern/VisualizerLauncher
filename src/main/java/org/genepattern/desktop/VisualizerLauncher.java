package org.genepattern.desktop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Created by nazaire on 4/3/16.
 */
public class VisualizerLauncher {
    private static final Logger log = LogManager.getLogger(VisualizerLauncher.class);
    
    public static final String GP_URL_DEFAULT = "https://genepattern.broadinstitute.org/gp";
    public static final String REST_API_JOB_PATH  = "/rest/v1/jobs";
    public static final String REST_API_TASK_PATH = "/rest/v1/tasks";

    private File downloadLocation;

    private JFrame frame;
    private JTextField serverField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField jobNumberField;
    private JLabel statusMsgField;

    private String gpServer;
    private JobInfo jobInfo;
    private String basicAuthHeader;

    VisualizerLauncher() {
        this.jobInfo = new JobInfo();
    }

    private void run() {
        JPanel panel = new JPanel(new GridLayout(4, 1));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        serverField = new JTextField(GP_URL_DEFAULT);
        TextPrompt serverFieldPrompt = new TextPrompt(GP_URL_DEFAULT, serverField);
        serverFieldPrompt.changeAlpha(0.4f);
        final JLabel serverLabel = new JLabel("server: ");
        serverLabel.setLabelFor(serverField);
        panel.add(serverLabel);
        panel.add(serverField);

        usernameField = new JTextField();
        final JLabel usernameLabel = new JLabel("username: ");
        usernameLabel.setLabelFor(usernameField);
        panel.add(usernameLabel);
        panel.add(usernameField);

        passwordField = new JPasswordField();
        final JLabel passwordLabel = new JLabel("password: ");
        passwordLabel.setLabelFor(passwordField);
        panel.add(passwordLabel);
        panel.add(passwordField);

        jobNumberField = new JTextField("");
        JLabel jobNumberLabel = new JLabel("job number: ");
        jobNumberLabel.setLabelFor(jobNumberField);
        panel.add(jobNumberLabel);
        panel.add(jobNumberField);

        JButton submit = new JButton("Submit");
        submit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String serverName = serverField.getText();
                String userName = usernameField.getText();
                char[] password = passwordField.getPassword();
                String jobNumber = jobNumberField.getText();

                if(serverName == null || serverName.length() == 0) {
                    displayMsg("Please enter a server", true);
                    statusMsgField.setText("");
                    return;
                }

                if (userName != null && userName.length() > 0) {
                    basicAuthHeader = Util.initBasicAuthHeader(userName, String.valueOf(password));
                }
                else {
                    displayMsg("Please enter a username", true);
                    statusMsgField.setText("");
                    return;
                }

                if(jobNumber == null || jobNumber.length() == 0) {
                    displayMsg("Please enter a job number", true);
                    statusMsgField.setText("");
                    return;
                }
                else {
                    try {
                        Integer.parseInt(jobNumber);
                    }
                    catch(NumberFormatException ne) {
                        displayMsg("Job number must be an integer", true);
                        statusMsgField.setText("");
                        return;
                    }
                }

                jobInfo = new JobInfo();
                jobInfo.setJobNumber(jobNumber);

                gpServer = serverName;
                if (serverName.endsWith("/")) {
                    // remove trailing slash
                    gpServer = serverName.substring(0, serverName.length()-1);
                }

                downloadLocation = FileUtil.getDownloadLocation(jobNumber);

                //setup location of log files
                String vizDirName = "visualizerLauncher";
                ThreadContext.put("logFileDir", vizDirName);
                ThreadContext.put("logFileName", "output");

                exec();
            }
        });

        JPanel submitPanel = new JPanel();
        submitPanel.setLayout(new BoxLayout(submitPanel, BoxLayout.Y_AXIS));
        submit.setAlignmentX(Component.CENTER_ALIGNMENT);
        submitPanel.add(submit);

        frame = new JFrame("GenePattern Java Visualizer Launcher");
        frame.setLayout(new BorderLayout());
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setPreferredSize(new Dimension(520, 270));
        frame.add(panel, BorderLayout.CENTER);
        frame.add(submitPanel, BorderLayout.SOUTH);

        JPanel statusMsgFieldPanel = new JPanel();
        statusMsgFieldPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        statusMsgFieldPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        statusMsgField = new JLabel();
        statusMsgFieldPanel.add(statusMsgField);
        submitPanel.add(statusMsgFieldPanel);
        frame.pack();

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width - frame.getWidth()) / 2, (screenSize.height - frame.getHeight()) / 2);
        frame.setVisible(true);
    }

    private void downloadSupportFiles(final GPTask task) throws Exception {
        for(final String supportFileURL : task.getSupportFileUrls()) {
            final int slashIndex = supportFileURL.lastIndexOf('=');
            final String filenameWithExtension =  supportFileURL.substring(slashIndex + 1);
            try {
                FileUtil.downloadFile(basicAuthHeader, new URL(supportFileURL), downloadLocation, filenameWithExtension);
            }
            catch (Throwable t) {
                throw new Exception("Error downloading support file: '"+supportFileURL+"'"+t.getMessage());
            }
        }
    }

    private void retrieveJobDetails(final String jobId) throws Exception {
        final String taskLsid=Util.retrieveJobDetails(basicAuthHeader, gpServer, jobId); 
        GPTask gpTask = new GPTask();
        gpTask.setLsid(taskLsid);
        jobInfo.setGpTask(gpTask);
    }

    /** Converts a string into something you can safely insert into a URL. */
    @SuppressWarnings("deprecation")
    public static String encodeURIcomponent(String str) {
        String encoded = str;
        try {
            encoded = URLEncoder.encode(str, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            encoded = URLEncoder.encode(str);
        }

        //replace all '+' with '%20'
        encoded = encoded.replace("+", "%20");
        return encoded;

    }

    private void retrieveTaskDetails() throws Exception
    {
        if(jobInfo == null || jobInfo.getGpTask() == null || jobInfo.getGpTask().getLsid() == null
                || jobInfo.getGpTask().getLsid().length() == 0)
        {
            throw new IllegalArgumentException("No valid task found");
        }

        String getTaskRESTCall = gpServer + REST_API_TASK_PATH  + "/" + jobInfo.getGpTask().getLsid() + "?includeSupportFiles=true";
        String response = Util.doGetRequest(basicAuthHeader, getTaskRESTCall);

        JSONTokener tokener = new JSONTokener(response);
        JSONObject root = new JSONObject(tokener);

        String commandLine = root.getString("command_line");

        if(commandLine == null || commandLine.length() == 0)
        {
            throw new Exception("No command line found for task with LSID: " + jobInfo.getGpTask().getLsid());
        }

        //check if this is a java visualizer
        if(!root.has("taskType"))
        {
            throw new Exception("No taskType property found. The taskType must be set to Visualizer");
        }

        String taskType = root.getString("taskType");
        if(!taskType.toLowerCase().equals("visualizer"))
        {
            throw new Exception("Unexpected taskType: " + taskType + ". Expecting the taskType to be \'Visualizer\'.");
        }

        if(!root.has("supportFiles"))
        {
            throw new Exception("No supportFiles property found. Please check if this is GenePattern version 3.9.8 or greater.");
        }

        JSONArray supportFileURIs = root.getJSONArray("supportFiles");

        if(supportFileURIs == null || supportFileURIs.length() == 0)
        {
            throw new Exception("No support files found for task with LSID: " + jobInfo.getGpTask().getLsid());
        }

        String[] supportFileURLs = new String[supportFileURIs.length()];
        for(int i=0;i<supportFileURIs.length();i++)
        {
            String supportFileURI = supportFileURIs.getString(i);
            String supportFileURL = gpServer +  supportFileURI;
            supportFileURLs[i] = supportFileURL;
        }

        GPTask gpTask = jobInfo.getGpTask();
        gpTask.setSupportFileUrls(supportFileURLs);
        gpTask.setCommandLine(commandLine);
    }

    protected static boolean isNullOrEmpty(final String str) {
        return str==null || str.length()==0;
    }
    
    /** helper method: surround each space-delimited token in double quotes */
    protected static String wrapTokensInQuotes(final String cmdLineIn) {
        String cmdLine="";
        String[] args=cmdLineIn.split(" ");
        for(final String argIn : args) {
            if (!isNullOrEmpty(argIn)) {
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

    private void prepareCommandLineStep() throws IOException {
        if (jobInfo == null 
                || jobInfo.getGpTask() == null 
                || jobInfo.getGpTask().getCommandLine() == null
                || jobInfo.getGpTask().getCommandLine().length() == 0
        ) {
            throw new IllegalArgumentException("No command line found");
        }
        String cmdLine = jobInfo.getGpTask().getCommandLine();
        //wrap args in double quotes
        cmdLine = wrapTokensInQuotes(cmdLine);
        //substitute <libdir> with the downloadLocation (aka local libdir)
        cmdLine = cmdLine.replace("<libdir>", downloadLocation.getAbsolutePath() + "/");
        //substitute <path.separator> on local VM, not necessarily the same as server 
        cmdLine = cmdLine.replace("<path.separator>", File.pathSeparator);
        //substitute <java> 
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        //add .exe extension if this is Windows
        java += (System.getProperty("os.name").startsWith("Windows") ? ".exe" : "");
        cmdLine = cmdLine.replace("<java>", java);

        //get the substituted commandline from the serverField
        final String getTaskRESTCall = 
                gpServer + REST_API_JOB_PATH  + "/" + jobInfo.getJobNumber() + "/visualizerCmdLine?commandline=" + encodeURIcomponent(cmdLine);
        final String response = Util.doGetRequest(basicAuthHeader, getTaskRESTCall);

        final JSONTokener tokener = new JSONTokener(response);
        final JSONObject root = new JSONObject(tokener);
        final JSONArray cmdLineArr = root.getJSONArray("commandline");
        log.debug("commandLine (from server): " + cmdLineArr);

        final Map<String, String> inputURLMap = jobInfo.getInputURLToFilePathMap();
        final String[] cmdLineList = new String[cmdLineArr.length()];
        for(int i=0;i< cmdLineArr.length(); i++) {
            String argValue = cmdLineArr.getString(i);
            if (argValue.startsWith("/gp/")) {
                // e.g. gpServer=http://127.0.0.1:8080/gp
                argValue=argValue.replaceFirst("/gp", gpServer);
            }
            if(inputURLMap.containsKey(argValue)) {
                argValue = downloadLocation.getAbsolutePath() + "/" + inputURLMap.get(argValue);
            }
            cmdLineList[i] = argValue;
        }

        log.debug("commandLine (local): " + Arrays.asList(cmdLineList));
        jobInfo.setCommandLine(cmdLineList);
    }

    private void runVisualizer() throws IOException {
        try {
            log.info("running command " + Arrays.asList(jobInfo.getCommandLine()));
            CommandUtil.runCommand(jobInfo.getCommandLine());
        } 
        catch (IOException e) {
            log.error("Error running visualizer command: "+jobInfo.getCommandLine(), e);
        }
    }

    protected void downloadInputFiles() throws Exception {
        if(jobInfo == null || jobInfo.getGpTask() == null || jobInfo.getGpTask().getCommandLine() == null
                || jobInfo.getGpTask().getCommandLine().length() == 0)
        {
            throw new IllegalArgumentException("No command line found");
        }

        // GET /gp/rest/v1/jobs/{jobId}/visualizerInputFiles{.json}
        final String inputFilesJson = 
                Util.doGetRequest(basicAuthHeader, gpServer + REST_API_JOB_PATH  + "/" + jobInfo.getJobNumber() + "/visualizerInputFiles");
        if (log.isTraceEnabled()) {
            log.trace(inputFilesJson);
        }

        final JSONObject inputFilesJsonObj=new JSONObject(inputFilesJson);
        final JSONArray inputFiles=inputFilesJsonObj.getJSONArray("inputFiles");
        final Map<String, String> map = new HashMap<String, String>();
        for(int i=0;i<inputFiles.length();i++) {
            final String inputFile = inputFiles.getString(i);
            final String inputFileUrlStr=initInputFileUrlStr(inputFile);
            final String filenameWithExtension=filenameWithExt(inputFileUrlStr);
            final URL fileURL = downloadInputFile(inputFileUrlStr);
            map.put(fileURL.toString(), filenameWithExtension);
        }
        jobInfo.setInputURLToFilePathMap(map);
    }

    protected String initInputFileUrlStr(final String inputFile) {
        if (inputFile.startsWith("<GenePatternURL>")) {
            return inputFile.replaceFirst("<GenePatternURL>", gpServer+"/");
        }
        else if (inputFile.startsWith("/gp/")) {
            // e.g. gpServer=http://127.0.0.1:8080/gp
            return inputFile.replaceFirst("/gp", gpServer);
        }
        else {
            return inputFile;
        }
    }

    protected String filenameWithExt(final String inputFile) {
        final int slashIndex = inputFile.lastIndexOf('/');
        final String filenameWithExtension =  inputFile.substring(slashIndex + 1);
        return filenameWithExtension;
    }

    protected URL downloadInputFile(final String inputFile)
            throws MalformedURLException, IOException {
        final String filenameWithExtension=filenameWithExt(inputFile);
        final URL fileURL = new URL(inputFile);
        FileUtil.downloadFile(basicAuthHeader, fileURL, downloadLocation, filenameWithExtension);
        return fileURL;
    }

    private void handleExecError(final String prefix, Throwable t) {
        String msg = prefix+": " + t.getLocalizedMessage();
        log.error(msg, t);
        statusMsgField.setText("");
        displayMsg(msg, true); 
    }

    private void exec() {
        String status="launching visualizer";
        try {
            log.info(status+" ...");
            log.info("     java.version: " + System.getProperty("java.version"));
            status="validating input";
            if (jobInfo == null || jobInfo.getJobNumber() == null) {
                throw new IllegalArgumentException("jobId not set");
            }
            log.info("     job number: " + jobInfo.getJobNumber());
            status="retrieving job details";
            log.info(status+" ...");
            retrieveJobDetails(jobInfo.getJobNumber());

            status="retrieving task details";
            log.info(status+" ...");
            retrieveTaskDetails();

            status="downloading support files";
            log.info(status+" ...");
            downloadSupportFiles(jobInfo.getGpTask());

            status="downloading input files";
            log.info(status+" ...");
            downloadInputFiles();
            
            status="preparing command line";
            log.info(status+" ...");
            prepareCommandLineStep();

            status="launching visualizer";
            log.info(status+" ...");
            runVisualizer();
        }
        catch (Throwable t) {
            handleExecError("Error "+status, t);
            return;
        }
    }

    private void displayMsg(final String msg, final boolean isError) {
        JTextArea jta = new JTextArea(msg);
        @SuppressWarnings("serial")
        JScrollPane scrollPane = new JScrollPane(jta){
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(410, 290);
            }
        };

        String type = "INFO";
        if(isError)
        {
            type = "ERROR: ";
        }
        JOptionPane.showMessageDialog(
                null, scrollPane, type, JOptionPane.ERROR_MESSAGE);
    }

    private static void createAndShowGUI() {
        VisualizerLauncher dsLauncher = new VisualizerLauncher();
        dsLauncher.run();
    }

    protected static void setDebugMode() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME); 
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();
    }

    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
