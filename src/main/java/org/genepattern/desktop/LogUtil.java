package org.genepattern.desktop;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogUtil {
    /** 
     * The 'log.dir' Java System Property is referenced in the 'log4j2.xml' config file. 
     * <pre>
         fileName="${sys:log.dir:-logs}/all.log"
     * </pre>
     */
    public static final String PROP_LOG_DIR="log.dir";
    
    public static void initLogging() {
        Singleton.INSTANCE.isInitialized();
    }
    
    public static Logger getLogger(final Class<?> clazz) {
        initLogging();
        return LogManager.getLogger(clazz);
    }

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
    private static void _initLogging() {
        try {
            final File logDir=new File(AppDirUtil.getAppDir(), "logs");
            System.setProperty(PROP_LOG_DIR, logDir.getAbsolutePath());
            org.apache.logging.log4j.core.LoggerContext ctx =
                (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            ctx.reconfigure();        
        }
        catch (Throwable t) {
            System.err.println("Unexpected error initializing logging");
            t.printStackTrace();
        }
    }

    // 
    // Thread safe initialization-on-demand ...
    //
    private static class Singleton {
        static final LogUtil INSTANCE=new LogUtil();
    }

    private LogUtil() {
        LogUtil._initLogging();
    }

    public boolean isInitialized() {
        return true;
    }

}
