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
import java.util.Arrays;

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

/**
 * Created by nazaire on 4/3/16.
 */
public class VisualizerLauncherGui {
    private JFrame frame;
    private JTextField serverField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField jobNumberField;
    private JLabel statusMsgField;

    private final File appDir;
    private GpServerInfo info;
    
    private final Logger log;

    VisualizerLauncherGui(final File appDir) {
        this.appDir=appDir;
        this.log=LogManager.getLogger(VisualizerLauncherGui.class);
    }

    private void run() {
        JPanel panel = new JPanel(new GridLayout(4, 1));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        serverField = new JTextField(GpServerInfo.GP_URL_DEFAULT);
        TextPrompt serverFieldPrompt = new TextPrompt(GpServerInfo.GP_URL_DEFAULT, serverField);
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

                if (Util.isNullOrEmpty(jobNumber)) {
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
                
                VisualizerLauncherGui.this.info=new GpServerInfo.Builder()
                    .gpServer(serverName)
                    .user(userName)
                    .pass(String.valueOf(password))
                    .jobNumber(jobNumber)
                    .appDir(appDir)
                .build();

                //setup location of log files
                ThreadContext.put("logFileDir", "visualizerLauncher");
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

    private void runVisualizer(final String[] cmdLineLocal) throws IOException {
        try {
            log.info("running command " + Arrays.asList(cmdLineLocal));
            CommandUtil.runCommand(cmdLineLocal);
        } 
        catch (IOException e) {
            log.error("Error running visualizer command: "+cmdLineLocal, e);
        }
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
            final JobInfo jobInfo = JobInfo.createFromJobId(info);

            status="retrieving task details";
            log.info(status+" ...");
            final TaskInfo taskInfo = TaskInfo.createFromLsid(info, jobInfo.getTaskLsid());

            status="downloading support files";
            log.info(status+" ...");
            taskInfo.downloadSupportFiles(info);

            status="downloading input files";
            log.info(status+" ...");
            jobInfo.downloadInputFiles(info);
            
            status="preparing command line";
            log.info(status+" ...");
            jobInfo.prepareCommandLineStep(info, taskInfo.getLibdir(), taskInfo.getCommandLine());

            status="launching visualizer";
            log.info(status+" ...");
            runVisualizer(jobInfo.getCmdLineLocal());
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

    protected static void createAndShowGUI(final File appDir) {
        VisualizerLauncherGui dsLauncher = new VisualizerLauncherGui(appDir);
        dsLauncher.run();
    }

    protected static void setDebugMode() {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        Configuration config = ctx.getConfiguration();
        LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME); 
        loggerConfig.setLevel(Level.DEBUG);
        ctx.updateLoggers();
    }

}
