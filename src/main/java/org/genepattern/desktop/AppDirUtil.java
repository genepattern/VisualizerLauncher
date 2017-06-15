package org.genepattern.desktop;

import java.io.File;

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
 * @author pcarr
 */
public class AppDirUtil {
    public static File getAppDir() {
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

    protected static File getAppDir_working_dir(final String jobNumber) {
        return new File("visualizerLauncher");
    }

    /**
     * Get the download location relative to the user.home directory:
     *     <user.home>/Library/visualizerLauncher/GenePattern_<jobNumber>
     */
    protected static File getAppDir_user_home(final String jobNumber) {
        final File home_dir=new File(System.getProperty("user.home"));
        return new File(home_dir, "Library/visualizerLauncher");
    }

}
