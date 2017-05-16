package org.genepattern.desktop;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.harawata.appdirs.AppDirs;
import net.harawata.appdirs.AppDirsFactory;

/**
 * Utility class for local file management.
 * 
 * By default data files are saved via the AppDirs framework into this location:
 *   On Mac OS X:   /Users/<Account>/Library/Application Support/VisualizerLauncher
 *   On Windows XP: C:\Documents and Settings\<Account>\Application Data\Local Settings\GenePattern\VisualizerLauncher
 *   On Windows 7:  C:\Users\<Account>\AppData\GenePattern\VisualizerLauncher
 *   On Unix/Linux: /home/<account>/.local/share/VisualizerLauncher
 * <AppAuthor> - GenePattern
 * <AppName>   - VisualizerLauncher 
 *   
 * @author pcarr
 */
public class FileUtil {
    private static final Logger log = LogManager.getLogger(FileUtil.class);

    /**
     * Download a URL to a local file and return a File object for it.
     *
     * @param url, The url to download.
     * @param dir, The directory to download the URL to.
     * @param filename, The filename to download the URL to.
     */
    public static File downloadFile(final String authString, final URL fromUrl, final File toDir, final String filename) throws IOException {
        final File toFile=new File(toDir, filename);
        return downloadFile(authString, fromUrl, toFile);
    }

    /**
     * Download a URL to a local file, mkdirs if necessary.
     * 
     * @param authString
     * @param fromUrl, the url to download
     * @param toFile, the local path to the file
     */
    public static File downloadFile(final String authString, final URL fromUrl, final File toFile) throws IOException {
        InputStream is = null;
        FileOutputStream fos = null;
        try {
            final File toDir=toFile.getParentFile();
            if (!toDir.exists()) {
                boolean created=toDir.mkdirs();
                if (log.isInfoEnabled()) {
                    if (created) {
                        log.info("     created directory: "+toDir);
                    }
                    else {
                        log.warn("     failed to create directory: "+toDir);
                    }
                }
            }
            URLConnection conn = fromUrl.openConnection();
            conn.setRequestProperty("Authorization", authString);
            is = conn.getInputStream();
            fos = new FileOutputStream(toFile);
            byte[] buf = new byte[100000];
            int j;
            while ((j = is.read(buf, 0, buf.length)) != -1) {
                fos.write(buf, 0, j);
            }
        }
        catch (IOException e) {
            log.error(e);
            throw e;
        }
        finally {
            if (is != null) {
                try {
                    is.close();
                }
                catch (IOException e) {
                    log.error("Unexpected error closing input stream, fromUrl="+fromUrl, e);
                }
            }
            if (fos != null) {
                try {
                    fos.close();
                }
                catch (IOException e) {
                    log.error("Unexpected error closing output stream, toFile="+toFile, e);
                }
            }
        }
        return toFile;
    }

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
