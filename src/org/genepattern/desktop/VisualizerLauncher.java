package org.genepattern.desktop;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.*;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;


/**
 * Created by nazaire on 4/3/16.
 */
public class VisualizerLauncher
{
    final static private Logger log = LogManager.getLogger(VisualizerLauncher.class);

    private File downloadLocation;
    private static String REST_API_JOB_PATH  = "/rest/v1/jobs";
    private static String REST_API_TASK_PATH = "/rest/v1/tasks";

    private JFrame frame;
    private JTextField serverField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField jobNumberField;
    private JLabel statusMsgField;

    private String gpServer;
    private JobInfo jobInfo;
    private String basicAuthString;

    VisualizerLauncher()
    {
        this.jobInfo = new JobInfo();

        addShutDownHook();
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

    private void login()
    {
        JPanel panel = new JPanel(new GridLayout(4, 1));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        serverField = new JTextField();
        TextPrompt serverFieldPrompt = new TextPrompt("http://localhost:8080/gp", serverField);
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

                if(serverName == null || serverName.length() == 0)
                {
                    displayMsg("Please enter a server", true);
                    statusMsgField.setText("");
                    return;
                }

                if (userName != null && userName.length() > 0) {
                    String authorizationString = userName + ":";

                    if (password != null && password.length != 0) {
                        authorizationString += String.valueOf(password);
                    }
                    byte[] authEncBytes = Base64.encodeBase64(authorizationString.getBytes());
                    basicAuthString = new String(authEncBytes);
                    basicAuthString = "Basic " + basicAuthString;
                }
                else
                {
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

    public void run()
    {
        login();
    }


    private void downloadSupportFiles(GPTask task)
    {
        try{

            String[] supportFileURLs = task.getSupportFileUrls();

            for(String supportFileURL : supportFileURLs)
            {
                int slashIndex = supportFileURL.lastIndexOf('=');
                String filenameWithExtension =  supportFileURL.substring(slashIndex + 1);

                Util.downloadFile(new URL(supportFileURL), downloadLocation, filenameWithExtension, basicAuthString);
            }
        }
        catch(MalformedURLException m)
        {
            m.printStackTrace();
        }
        catch(IOException io)
        {
            io.printStackTrace();
        }
    }

    private String doGetRequest(String URL) throws IOException
    {
        String responseBody = "";
        CloseableHttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpGet httpget = new HttpGet(URL);
            httpget.setHeader("Authorization", basicAuthString);

            log.info("Executing request " + httpget.getRequestLine());

            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }

            };
            responseBody = httpClient.execute(httpget, responseHandler);
            log.info("----------------------------------------");
            log.info(responseBody);

        } finally {
            httpClient.close();
        }

        return responseBody;
    }

    private void retrievejobDetails() throws Exception
    {
        if(jobInfo == null || jobInfo.getJobNumber() == null)
        {
            throw new IllegalArgumentException("No valid job found");
        }
        String getJobAPICall = gpServer + VisualizerLauncher.REST_API_JOB_PATH + "/" + jobInfo.getJobNumber();
        String response = doGetRequest(getJobAPICall);

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
        String response = doGetRequest(getTaskRESTCall);

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

        String response = doGetRequest(getTaskRESTCall);

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
            e.printStackTrace();

            String msg = "An error occurred while running the visualizer: "
                    + e.getLocalizedMessage();
            log.error(msg);
            displayMsg(msg, true);
            return;
        }
    }

    private void downloadInputFiles() throws Exception
    {
        if(jobInfo == null || jobInfo.getGpTask() == null || jobInfo.getGpTask().getCommandLine() == null
                || jobInfo.getGpTask().getCommandLine().length() == 0)
        {
            throw new IllegalArgumentException("No command line found");
        }

        //get the URLs to the input files for the job
        String getJobInputFilesRESTCall = gpServer + REST_API_JOB_PATH  + "/" + jobInfo.getJobNumber() + "/visualizerInputFiles";

        String response = doGetRequest(getJobInputFilesRESTCall);

        JSONTokener tokener = new JSONTokener(response);
        JSONObject root = new JSONObject(tokener);

        JSONArray inputFiles = root.getJSONArray("inputFiles");

        Map<String, String> inputURLToFilePathMap = new HashMap<String, String>();

        for(int i=0;i<inputFiles.length();i++)
        {
            String inputFileURL = inputFiles.getString(i);
            int slashIndex = inputFileURL.lastIndexOf('/');
            String filenameWithExtension =  inputFileURL.substring(slashIndex + 1);

            URL fileURL = new URL(inputFileURL);
            inputURLToFilePathMap.put(fileURL.toString(), filenameWithExtension);
            Util.downloadFile(fileURL, downloadLocation, filenameWithExtension, basicAuthString);
        }

        jobInfo.setInputURLToFilePathMap(inputURLToFilePathMap);
    }

    private void exec()
    {
        try
        {
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
        catch(Exception e)
        {
            log.error(e);
            String msg = "An error occurred while running the visualizer: "
                    + e.getLocalizedMessage();
            log.error(msg);
            statusMsgField.setText("");
            displayMsg(msg, true);
        }
    }

    public void runCommand(final String[] command) throws IOException
    {
        Thread t = new Thread() {
            public void run() {

                Process process = null;
                try {
                    ProcessBuilder probuilder = new ProcessBuilder(command);
                    //You can set up your work directory
                    //probuilder.directory(new File(currentdirectory.getAbsolutePath()));

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

                //Read out dir output
                InputStream is = process.getInputStream();
                InputStreamReader isr = new InputStreamReader(is);


                // drain the output and error streams
                copyStream(process.getInputStream(), System.out);
                copyStream(process.getErrorStream(), System.err);

                //Wait to get exit value
                try {
                    int exitValue = process.waitFor();
                    //System.exit(exitValue);
                    //frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }

    private void displayMsg(String msg, boolean isError) {
        JTextArea jta = new JTextArea(msg);
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

    public void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                String[] entries = downloadLocation.list();
                if(entries != null)
                {
                    for(String s: entries){
                        File currentFile = new File(downloadLocation.getPath(),s);
                       // currentFile.delete();
                    }
                    //downloadLocation.delete();
                }
            }
        });
    }

    public static void main(String[] args)
    {
        VisualizerLauncher dsLauncher = new VisualizerLauncher();
        dsLauncher.run();
    }
}
