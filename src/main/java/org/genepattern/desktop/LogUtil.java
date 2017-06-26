package org.genepattern.desktop;

import java.io.File;

import org.apache.logging.log4j.LogManager;

public class LogUtil {
    /** 
     * The 'log.dir' Java System Property is referenced in the 'log4j2.xml' config file. 
     * <pre>
         fileName="${sys:log.dir:-logs}/all.log"
     * </pre>
     */
    public static final String PROP_LOG_DIR="log.dir";
    
    /**
     * Set the log4j2 log directory at application startup, so that log files
     * are saved in the same location as other application files.
     * See the AppDirUtil javadoc for details.
     * 
     * For best results, call this only once, on application startup, before
     * initializing a logger.
     * 
     * Note: it's called from a private constructor as part of 
     * the initialization-on-demand idiom.
     */
    public static void initLogging(final File appDir) {
        // special-case: short-circuit if 'log.dir' is already set
        if (Util.isPropertySet(PROP_LOG_DIR)) {
            System.out.println("log.dir="+System.getProperty(PROP_LOG_DIR));
            return;
        }
        // set to appDir/logs
        try {
            // special-case: use current working directory if appDir is null
            final File logDir;
            if (appDir==null) {
                logDir=new File("logs");
            }
            else {
                logDir=new File(appDir, "logs");
            }
            System.setProperty(PROP_LOG_DIR, logDir.getPath());
            System.out.println("log.dir="+System.getProperty(PROP_LOG_DIR));

            org.apache.logging.log4j.core.LoggerContext ctx =
                (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            ctx.reconfigure();        
        }
        catch (Throwable t) {
            System.err.println("Unexpected error initializing logging");
            t.printStackTrace();
        }
    }

}
