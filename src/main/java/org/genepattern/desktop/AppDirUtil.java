package org.genepattern.desktop;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

/**
 * Helper class to get the user data directory.
 * Data files are saved via the AppDirs framework into this location:
 *   On Mac OS X:   /Users/<Account>/Library/Application Support/VisualizerLauncher
 *   On Windows XP: C:\Documents and Settings\<Account>\Application Data\Local Settings\GenePattern\VisualizerLauncher
 *   On Windows 7:  C:\Users\<Account>\AppData\GenePattern\VisualizerLauncher
 *   On Unix/Linux: /home/<account>/.local/share/VisualizerLauncher
 * <AppAuthor> - GenePattern
 * <AppName>   - VisualizerLauncher 
 * 
 * Note: this location can be customized with the -Duser.data.dir java command line flag, e.g.
 *     java -Duser.data.dir=~/visualizerLauncher ...
 * @author pcarr
 */
public class AppDirUtil {
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

    public static File getAppDir() {
        return getAppDir(System.getProperty(PROP_USER_DATA_DIR));
    }

    protected static File getAppDir(final String userDataDir) {
        if (userDataDir!=null) {
            //special-case: initialize from 'user.data.dir' property
            File f=getAppDir_system_prop(userDataDir);
            if (f!=null) {
                return f;
            }
        }
        return getAppDir_standard();
    }

    /**
     * Get the download location using the AppDirs API.
     * @param jobNumber
     * @return
     */
    protected static File getAppDir_standard() {
        final AppDirs appDirs = AppDirsFactory.getInstance();
        final String appName="VisualizerLauncher";
        final String appVersion="";
        final String appAuthor="GenePattern";
        return new File(appDirs.getUserDataDir(appName, appVersion, appAuthor));
    }

    protected static File getAppDir_system_prop(String userDataDir) {
        if (userDataDir==null) {
            return null;
        }
        userDataDir=userDataDir.trim();
        try {
            if (userDataDir.startsWith("~")) {
                userDataDir=userDataDir.replaceFirst("~", System.getProperty("user.home"));
            }
            final Path path=FileSystems.getDefault().getPath(userDataDir).toAbsolutePath().normalize();
            return path.toAbsolutePath().normalize().toFile();
        }
        catch (Throwable t) {
            System.err.println("Error initializing user.data.dir from System.getProperty('user.data.dir')='"+userDataDir+"'");
            t.printStackTrace();
            return null;
        }
    }

    protected static File getAppDir_working_dir() {
        return new File("visualizerLauncher");
    }

    /**
     * Get the download location relative to the user.home directory:
     *     <user.home>/Library/visualizerLauncher/GenePattern_<jobNumber>
     */
    protected static File getAppDir_user_home() {
        final File home_dir=new File(System.getProperty("user.home"));
        return new File(home_dir, "Library/visualizerLauncher");
    }

}
