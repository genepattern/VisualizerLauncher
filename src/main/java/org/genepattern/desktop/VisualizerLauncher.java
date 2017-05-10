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
    private File libdir;

    private JFrame frame;
    private JTextField serverField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField jobNumberField;
    private JLabel statusMsgField;
    
    private GpServerInfo info;

    VisualizerLauncher() {
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

                if (Util.isNullOrEmpty(serverName)) {
                    displayMsg("Please enter a server", true);
                    statusMsgField.setText("");
                    return;
                }

                if (Util.isNullOrEmpty(userName)) {
                    displayMsg("Please enter a username", true);
                    statusMsgField.setText("");
                    return;
                }

                if(Util.isNullOrEmpty(jobNumber)) {
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
                
                VisualizerLauncher.this.info=new GpServerInfo.Builder()
                        .gpServer(serverName)
                        .user(userName)
                        .pass(String.valueOf(password))
                        .jobNumber(jobNumber)
                    .build();

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

    private String[] prepareCommandLineStep(final Map<String, String> inputURLMap, final String commandLine) throws IOException {
        if (commandLine==null) {
            throw new IllegalArgumentException("commandLine==null");
        }
        if (inputURLMap==null) {
            throw new IllegalArgumentException("inputURLMap==null");
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
                info.getGpServer() + REST_API_JOB_PATH  + "/" + info.getJobNumber() + "/visualizerCmdLine?commandline=" + encodeURIcomponent(cmdLine);
        final String response = Util.doGetRequest(info.getBasicAuthHeader(), getTaskRESTCall);

        final JSONTokener tokener = new JSONTokener(response);
        final JSONObject root = new JSONObject(tokener);
        final JSONArray cmdLineArr = root.getJSONArray("commandline");
        log.debug("commandLine (from server): " + cmdLineArr);

        final String[] commandLineLocal = new String[cmdLineArr.length()];
        for(int i=0;i< cmdLineArr.length(); i++) {
            String argValue = cmdLineArr.getString(i);
            if (argValue.startsWith("/gp/")) {
                // e.g. gpServer=http://127.0.0.1:8080/gp
                argValue=argValue.replaceFirst("/gp", info.getGpServer());
            }
            if(inputURLMap.containsKey(argValue)) {
                argValue = downloadLocation.getAbsolutePath() + "/" + inputURLMap.get(argValue);
            }
            commandLineLocal[i] = argValue;
        }

        log.debug("commandLine (local): " + Arrays.asList(commandLineLocal));
        return commandLineLocal;
    }

    private void runVisualizer(final String[] cmdLineLocal) throws IOException {
        try {
            log.info("running command " + Arrays.asList(cmdLineLocal));
            CommandUtil.runCommand(cmdLineLocal);
        } 
        catch (IOException e) {
            log.error("Error running visualizer command: "+cmdLineLocal, e);
        }
    }

    /**
     * @return Map<String,String> inputUrlToFilePathMap
     */
    protected Map<String,String> downloadInputFiles() throws Exception {
        if (info == null) {
            throw new IllegalArgumentException("info==null");
        }
        if (Util.isNullOrEmpty(info.getJobNumber())) {
            throw new IllegalArgumentException("job number not set");
        }

        // GET /gp/rest/v1/jobs/{jobId}/visualizerInputFiles{.json}
        final String inputFilesJson = Util.doGetRequest(
                info.getBasicAuthHeader(), 
                info.getGpServer() + REST_API_JOB_PATH  + "/" + info.getJobNumber() + "/visualizerInputFiles");
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
        return map;
    }

    protected String initInputFileUrlStr(final String inputFile) {
        if (inputFile.startsWith("<GenePatternURL>")) {
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

    protected String filenameWithExt(final String inputFile) {
        final int slashIndex = inputFile.lastIndexOf('/');
        final String filenameWithExtension =  inputFile.substring(slashIndex + 1);
        return filenameWithExtension;
    }

    protected URL downloadInputFile(final String inputFile)
            throws MalformedURLException, IOException {
        final String filenameWithExtension=filenameWithExt(inputFile);
        final URL fileURL = new URL(inputFile);
        FileUtil.downloadFile(info.getBasicAuthHeader(), fileURL, downloadLocation, filenameWithExtension);
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
            if (info == null || info.getJobNumber() == null) {
                throw new IllegalArgumentException("job number not set");
            }
            log.info("     job number: " + info.getJobNumber());
            status="retrieving job details";
            log.info(status+" ...");
            final String taskLsid=Util.retrieveJobDetails(info.getBasicAuthHeader(), info.getGpServer(), info.getJobNumber());

            status="retrieving task details";
            log.info(status+" ...");
            TaskInfo taskInfo = TaskInfo.createFromLsid(info, taskLsid);
            this.libdir=taskInfo.getLibdir();

            status="downloading support files";
            log.info(status+" ...");
            taskInfo.downloadSupportFiles(info);

            status="downloading input files";
            log.info(status+" ...");
            final Map<String,String> inputURLToFilePathMap=downloadInputFiles();
            
            status="preparing command line";
            log.info(status+" ...");
            final String[] cmdLineLocal = prepareCommandLineStep(inputURLToFilePathMap, taskInfo.getCommandLine());

            status="launching visualizer";
            log.info(status+" ...");
            runVisualizer(cmdLineLocal);
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
