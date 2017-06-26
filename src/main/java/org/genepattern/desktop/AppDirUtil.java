package org.genepattern.desktop;

import static org.genepattern.desktop.VisualizerLauncher.PROP_USER_DATA_DIR;

import java.io.File;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static File initAppDir() {
        return initAppDir(System.getProperty(PROP_USER_DATA_DIR));
    }

    protected static File initAppDir(final String userDataDir) {
        if (userDataDir!=null) {
            // special-case: -Duser.data.dir=<userDataDir>
            return getAppDir_system_prop(userDataDir);
        }
        else {
            // default: use appdirs framework
            return getAppDir_standard();
        }
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

    /** 
     * special-case: expand "~" at beginning of file path to 'user.home', e.g.
     *   ~/foo
     * Note: home directory to other users will be ignored, e.g.
     *   ~test_user/foo 
     * will not expand
     */
    protected static String expandTildePrefix(final String str) {
        if (   str.equals("~") 
            || str.startsWith("~/")
            || str.startsWith("~"+File.separatorChar) 
        ) {
            return str.replaceFirst(
                Pattern.quote("~"), 
                Matcher.quoteReplacement(System.getProperty("user.home")));
        }
        return str;
    }
    
    protected static boolean isCurrentDir(final String str) {
        if (   str==null
            || str.equals("")
            || str.equals(".") 
            || str.equals("./")
            || str.equals("."+File.separator)
        ) {
            return true;
        }
        return false;
    }
    
    protected static File getAppDir_system_prop(String userDataDir) {
        userDataDir=userDataDir.trim();
        if (isCurrentDir(userDataDir)) {
            return null;
        }
        userDataDir=expandTildePrefix(userDataDir);
        try {
            return Paths.get(userDataDir).normalize().toFile();
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

}
