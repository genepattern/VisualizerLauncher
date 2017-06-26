package org.genepattern.desktop;

import java.io.File;

/**
 * This is a rewrite of the JobInfo class, holds input values
 * for a single run of the visualizer launcher.
 */
public class GpServerInfo {
    public static final String GP_URL_DEFAULT = "https://genepattern.broadinstitute.org/gp";

    protected static boolean isNullOrEmpty(final File file) {
        if (file==null) {
            return true;
        }
        else if (file.toString()=="") {
            return true;
        }
        return false;
    }

    private final String basicAuthHeader;
    private final String gpServer;
    private final String jobNumber;
    
    private final File appDir;

    private GpServerInfo(final Builder in) {
        this.gpServer=in.gpServer;
        this.basicAuthHeader=Util.initBasicAuthHeader(in.user, in.pass);
        this.jobNumber=in.jobNumber;
        this.appDir=in.appDir;
    }

    public String getBasicAuthHeader() {
        return basicAuthHeader;
    }

    public String getGpServer() {
        return gpServer;
    }
    
    public String getJobNumber() {
        return jobNumber;
    }

    public File getDataDir(final String relativePath) {
        if (isNullOrEmpty(appDir)) {
            return new File(relativePath).toPath().normalize().toFile();
        }
        else {
            return new File(appDir, relativePath).toPath().normalize().toFile();
        }
    }

    /**
     * Get the local directory for job input files,
     *   default: <appDir>/jobs/<jobId>
     */
    public File getLocalJobDir() {
        return getDataDir("jobs/"+jobNumber);
    }
    
    public static class Builder {
        private String user;
        private String pass;
        private String gpServer;
        private String jobNumber;
        private File appDir;

        public Builder gpServer(final String gpServer) {
            this.gpServer=gpServer;
            if (this.gpServer.endsWith("/")) {
                // remove trailing slash
                this.gpServer = this.gpServer.substring(0, this.gpServer.length()-1);
            }
            return this;
        }

        public Builder user(final String user) {
            this.user=user;
            return this;
        }

        public Builder pass(final String pass) {
            this.pass=pass;
            return this;
        }
        
        public Builder jobNumber(final String jobNumber) {
            this.jobNumber=jobNumber;
            return this;
        }
        
        public Builder appDir(final File appDir) {
            this.appDir=appDir;
            return this;
        }

        public GpServerInfo build() {
            return new GpServerInfo(this);
        }
    }

}
