package org.genepattern.desktop;

import java.io.File;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    
    /** helper method because String#replaceAll expects a regex */
    protected static String replaceAll_quoted(final String str, final String literal, final String replacement) {
        if (Util.isNullOrEmpty(str)) {
            return str;
        }
        return str.replaceAll(
            Pattern.quote(literal),
            Matcher.quoteReplacement(replacement)
        );
    }

    /** helper method because String#replaceFirst expects a regex */
    protected static String replaceFirst_quoted(final String str, final String arg, final String replacement) {
        if (Util.isNullOrEmpty(str)) {
            return str;
        }
        return str.replaceFirst(
                Pattern.quote(arg),
                Matcher.quoteReplacement(replacement));
    }

    /**
     * Substitute '<GenePatternURL>' if necessary. 
     * Special case for a visualizer in a pipeline. E.g.
     *   inputFile="<GenePatternURL>jobResults/123/all_aml_test.gct" 
     */
    protected static String substituteGpUrl(final GpServerInfo info, final String inputFile) {
        if (inputFile.startsWith("<GenePatternURL>/")) {
            return replaceFirst_quoted(inputFile, "<GenePatternURL>/", info.getGpServer()+"/");
        }
        else if (inputFile.startsWith("<GenePatternURL>")) {
            return replaceFirst_quoted(inputFile, "<GenePatternURL>", info.getGpServer()+"/");
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
    
    /**
     * Get the local path to this inputFile
     * 
     *   <parentDir>/filename
     *   
     * @param parentDir, the local parent directory for example the local
     *     path to the job input files
     */
    protected String toLocalPath(final File parentDir) {
        if (parentDir==null) {
            // ignore
            return filename;
        }
        else {
            //TODO: handle relative path
            //  return new File(parentDir, filename).getPath();
            return new File(parentDir, filename).getAbsolutePath();
        }
    }
    
    /**
     * Substitute the inputFile url with the local file path to
     * the given parent directory.
     */
    public String substituteLocalPath(final String cmdLineArg, final File parentDir) {
        final String localPath=toLocalPath(parentDir);
        return substituteLocalPath(cmdLineArg, localPath);
    }

    protected String substituteLocalPath(final String cmdLineArg, final String localPath) {
        return replaceAll_quoted(cmdLineArg, arg, localPath);
    }

}
