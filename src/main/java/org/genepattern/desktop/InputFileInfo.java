package org.genepattern.desktop;

import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class for visualizer input file values returned from the REST API
 *   GET /rest/v1/jobs/{jobId}/visualizerInputFiles
 * 
 * @author pcarr
 */
public class InputFileInfo {
    private static final Logger log = LogManager.getLogger(InputFileInfo.class);
    
    /**
     * Substitute '<GenePatternURL>' if necessary. 
     * Special case for a visualizer in a pipeline. E.g.
     *   inputFile="<GenePatternURL>jobResults/123/all_aml_test.gct" 
     */
    protected static String substituteGpUrl(final GpServerInfo info, final String inputFile) {
        if (inputFile.startsWith("<GenePatternURL>/")) {
            return inputFile.replaceFirst("<GenePatternURL>/", info.getGpServer()+"/");
        }
        else if (inputFile.startsWith("<GenePatternURL>")) {
            return inputFile.replaceFirst("<GenePatternURL>", info.getGpServer()+"/");
        }
        else {
            return inputFile;
        }
    }

    /**
     * Prepend the GenePatternURL if necessary.
     * Workaround for bug in 'Send to Module' from job status page, E.g.
     *   inputFile="/gp/jobResults/123/all_aml_test.gct" 
     */
    protected static String prependGpUrl(final GpServerInfo info, final String inputFile) {
        if (inputFile.startsWith("/gp/")) {
            // e.g. gpServer=http://127.0.0.1:8080/gp
            return inputFile.replaceFirst("/gp", info.getGpServer());
        }
        else {
            return inputFile;
        }        
    }

    protected static String getFilenameFromUrl(final String fromUrl) {
        String path;
        try {
            path=new URL(fromUrl).toURI().getPath();
        }
        catch (Throwable t) {
            log.error("Error converting url to file path, fromUrl='"+fromUrl+"'", t);
            path=fromUrl;
        }
        final int idx = path.lastIndexOf('/');
        final String filename = path.substring(idx + 1);
        return filename;
    }
    
    public InputFileInfo(final GpServerInfo info, final String inputFile) {
        if (info==null) {
            throw new IllegalArgumentException("info==null");
        }
        if (inputFile==null) {
            throw new IllegalArgumentException("inputFile==null");
        }
        if (inputFile.length()==0) {
            throw new IllegalArgumentException("inputFile is empty");
        }
        this.arg=substituteGpUrl(info, inputFile);
        this.url=prependGpUrl(info, this.arg);
        this.filename=getFilenameFromUrl(url);
    }

    /** the 'inputFile' value returned from the REST API */
    final String arg;
    /** the fully qualified url to download the file from the server */
    final String url;
    /** the local name for the file */
    final String filename;
    
    public String getArg() {
        return arg;
    }

    public String getUrl() {
        return url;
    }

    public String getFilename() {
        return filename;
    }

}
