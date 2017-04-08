package org.genepattern.desktop;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Created by nazaire on 4/3/16.
 */
public class VisualizerLauncher {
    final static private Logger log = LogManager.getLogger(VisualizerLauncher.class);

    private File downloadLocation;
    public static String REST_API_JOB_PATH  = "/rest/v1/jobs";
    public static String REST_API_TASK_PATH = "/rest/v1/tasks";

    private JFrame frame;
    private JTextField serverField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField jobNumberField;
    private JLabel statusMsgField;

    private String gpServer;
    private JobInfo jobInfo;
    private String basicAuthString;

    VisualizerLauncher() {
        this.jobInfo = new JobInfo();
    }

    protected Thread copyStream(final InputStream is, final PrintStream out) {
        // create thread to read from the a process output or error stream
        Thread copyThread = new Thread(new Runnable() {
            public void run() {
                BufferedReader in = new BufferedReader(new InputStreamReader(is));
                String line;
                try {
                    while ((line = in.readLine()) != null) {
                        out.println(line);
                    }
                } catch (IOException ioe) {
                    System.err.println("Error reading from process stream.");
                }
            }
        });
        copyThread.setDaemon(true);
        copyThread.start();
        return copyThread;
    }

    private void login() {
        JPanel panel = new JPanel(new GridLayout(4, 1));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        serverField = new JTextField("https://genepattern.broadinstitute.org/gp");
        TextPrompt serverFieldPrompt = new TextPrompt("https://genepattern.broadinstitute.org/gp", serverField);
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
                    //String authorizationString = userName + ":";

                    //if (password != null && password.length != 0) {
                    //    authorizationString += String.valueOf(password);
                    //}
                    //byte[] authEncBytes = Base64.encodeBase64(authorizationString.getBytes());
                    //basicAuthString = new String(authEncBytes);
                    //basicAuthString = "Basic " + basicAuthString;
                    basicAuthString = Util.initBasicAuthString(userName, password);
                }
                else {
                    displayMsg("Please enter a username", true);
                    statusMsgField.setText("");
                    return;
                }

                if(jobNumber == null || jobNumber.length() == 0)
                {
                    displayMsg("Please enter a job number", true);
                    statusMsgField.setText("");
                    return;
                }
                else
                {
                    try
                    {
                        Integer.parseInt(jobNumber);
                    }
                    catch(NumberFormatException ne)
                    {
                        displayMsg("Job number must be an integer", true);
                        statusMsgField.setText("");
                        return;
                    }
                }

                jobInfo = new JobInfo();
                jobInfo.setJobNumber(jobNumber);

                String topLevelOuput = "visualizerLauncher";
                String outputDir = "GenePattern_" + jobNumber;

                gpServer = serverName;
                if (serverName.endsWith("/")) {
                    // remove trailing slash
                    gpServer = serverName.substring(0, serverName.length()-1);
                }

                downloadLocation = new File(topLevelOuput, outputDir);

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

    public void run() {
        login();
    }

    private void downloadSupportFiles(final GPTask task) throws Exception {
        for(final String supportFileURL : task.getSupportFileUrls()) {
            final int slashIndex = supportFileURL.lastIndexOf('=');
            final String filenameWithExtension =  supportFileURL.substring(slashIndex + 1);
            try {
                Util.downloadFile(basicAuthString, new URL(supportFileURL), downloadLocation, filenameWithExtension);
            }
            catch (Throwable t) {
                throw new Exception("Error downloading support file: '"+supportFileURL+"'"+t.getMessage());
            }
        }
    }

    private void retrievejobDetails() throws Exception
    {
        if(jobInfo == null || jobInfo.getJobNumber() == null)
        {
            throw new IllegalArgumentException("No valid job found");
        }
        String getJobAPICall = gpServer + VisualizerLauncher.REST_API_JOB_PATH + "/" + jobInfo.getJobNumber();
        String response = Util.doGetRequest(basicAuthString, getJobAPICall);

        JSONTokener tokener = new JSONTokener(response);
        JSONObject root = new JSONObject(tokener);

        String taskLsid = root.getString("taskLsid");

        if(taskLsid == null || taskLsid.length() == 0)
        {
            throw new Exception("Task lsid was not found");
        }

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
        String response = Util.doGetRequest(basicAuthString, getTaskRESTCall);

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

    private void runVisualizer() throws  Exception
    {
        if(jobInfo == null || jobInfo.getGpTask() == null || jobInfo.getGpTask().getCommandLine() == null
                || jobInfo.getGpTask().getCommandLine().length() == 0)
        {
            throw new IllegalArgumentException("No command line found");
        }

        String cmdLine = jobInfo.getGpTask().getCommandLine();

        //substitute <libdir> on the commandline with empty string since
        //all support files are in the current directory
        cmdLine = cmdLine.replace("<libdir>", downloadLocation.getAbsolutePath() + "/");

        //substitute the <java> on the command
        String java = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        //add .exe extension if this is Windows
        java += (System.getProperty("os.name").startsWith("Windows") ? ".exe" : "");
        cmdLine = cmdLine.replace("<java>", "\"" + java + "\"");

        //get the substituted commandline from the serverField
        String getTaskRESTCall = gpServer + REST_API_JOB_PATH  + "/" + jobInfo.getJobNumber() + "/visualizerCmdLine?commandline=" + encodeURIcomponent(cmdLine);

        String response = Util.doGetRequest(basicAuthString, getTaskRESTCall);

        JSONTokener tokener = new JSONTokener(response);
        JSONObject root = new JSONObject(tokener);

        JSONArray cmdLineArr = root.getJSONArray("commandline");
        log.info("commandline: " + cmdLineArr);

        Map<String, String> inputURLMap = jobInfo.getInputURLToFilePathMap();
        String[] cmdLineList = new String[cmdLineArr.length()];
        for(int i=0;i< cmdLineArr.length(); i++)
        {
            String argValue = cmdLineArr.getString(i);
            if(inputURLMap.containsKey(argValue))
            {
                argValue = downloadLocation.getAbsolutePath() + "/" + inputURLMap.get(argValue);
            }

            cmdLineList[i] = argValue;
        }

        log.info("running command " + Arrays.asList(cmdLineList));
        jobInfo.setCommandLine(cmdLineList);
        try {
            runCommand(jobInfo.getCommandLine());
            statusMsgField.setText("");
        }
        catch (IOException e) {
            final String msg = "An error occurred while running the visualizer: " + e.getLocalizedMessage();
            log.error(msg, e);
            displayMsg(msg, true);
            return;
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
                getVisualizerInputFilesJson(basicAuthString, gpServer, jobInfo.getJobNumber());
        final JSONObject inputFilesJsonObj=new JSONObject(inputFilesJson);
        final JSONArray inputFiles=inputFilesJsonObj.getJSONArray("inputFiles");
        final Map<String, String> map = new HashMap<String, String>();
        for(int i=0;i<inputFiles.length();i++) {
            String inputFile = inputFiles.getString(i);
            if (inputFile.startsWith("<GenePatternURL>")) {
                inputFile=inputFile.replaceFirst("<GenePatternURL>", gpServer+"/");
            }
            final String filenameWithExtension=filenameWithExt(inputFile);
            final URL fileURL = downloadInputFile(inputFile);
            map.put(fileURL.toString(), filenameWithExtension);
        }
        jobInfo.setInputURLToFilePathMap(map);
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
        Util.downloadFile(basicAuthString, fileURL, downloadLocation, filenameWithExtension);
        return fileURL;
    }

    protected static String getVisualizerInputFilesJson(final String basicAuth, final String gpServer, final String jobId)
    throws Exception 
    {
        //get the URLs to the input files for the job
        final String fromUrl = gpServer + REST_API_JOB_PATH  + "/" + jobId + "/visualizerInputFiles";
        try {
            return Util.doGetRequest(basicAuth, fromUrl);
        }
        catch (Throwable t) {
            throw new Exception("Error in GET "+fromUrl+": "+t.getMessage());
        }
    }

    private void exec() {
        try {
            //retrieve the job details in order to get the task details
            statusMsgField.setText("Retrieving job details...");
            retrievejobDetails();

            //retrieve the visualizer module details
            statusMsgField.setText("Retrieving task details...");
            retrieveTaskDetails();

            //download the support files
            statusMsgField.setText("Downloading support files...");
            downloadSupportFiles(jobInfo.getGpTask());

            //download the input files
            statusMsgField.setText("Downloading input files...");
            downloadInputFiles();

            //run the visualizer
            statusMsgField.setText("Launching visualizer...");
            runVisualizer();
        }
        catch (Exception e) {
            String msg = "An error occurred while running the visualizer: " + e.getLocalizedMessage();
            log.error(msg, e);
            statusMsgField.setText("");
            displayMsg(msg, true);
        }
    }

    public void runCommand(final String[] command) throws IOException {
        Thread t = new Thread() {
            public void run() {
                Process process = null;
                try {
                    ProcessBuilder probuilder = new ProcessBuilder(command);
                    process = probuilder.start();
                }
                catch (IOException e1) {
                    e1.printStackTrace();

                    String msg = "An error occurred while running the visualizer: " + e1.getLocalizedMessage();
                    log.error(msg);
                    statusMsgField.setText("");
                    displayMsg(msg, true);

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

    private void displayMsg(String msg, boolean isError) {
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

    public static void main(String[] args)
    {
        VisualizerLauncher dsLauncher = new VisualizerLauncher();
        dsLauncher.run();
    }
}
