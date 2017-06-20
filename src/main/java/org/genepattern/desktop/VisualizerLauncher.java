package org.genepattern.desktop;

import java.io.File;

public class VisualizerLauncher {

    /** 
     * Set the 'user.data.dir' Java System Property to change the default location for user data. 
     * Usage:
     *   # empty string means current working directory
     *   java -Duser.data.dir="" ...
     *   
     *   # relative path means, relative to current working directory
     *   java -Duser.data.dir="visualizerLauncher" ...
     */
    public static final String PROP_USER_DATA_DIR="user.data.dir";

    
    /*
Application startup workflow ...
  1) init application directory
  2) init logging
  3) open gui
     */
    
    public static void main(String[] args) {
        final File appDir=AppDirUtil.initAppDir();
        LogUtil.initLogging(appDir);
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                VisualizerLauncherGui.createAndShowGUI(appDir);
            }
        });
    }
}
