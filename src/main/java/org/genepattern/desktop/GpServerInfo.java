package org.genepattern.desktop;

/**
 * This is a rewrite of the JobInfo class, holds input values
 * for a single run of the visualizer launcher.
 */
public class GpServerInfo {
    public static final String GP_URL_DEFAULT = "https://genepattern.broadinstitute.org/gp";

    private final String basicAuthHeader;
    private final String gpServer;
    private final String jobNumber;

    private GpServerInfo(final Builder in) {
        this.gpServer=in.gpServer;
        this.basicAuthHeader=Util.initBasicAuthHeader(in.user, in.pass);
        this.jobNumber=in.jobNumber;
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

    public static class Builder {
        private String user;
        private String pass;
        private String gpServer;
        private String jobNumber;

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

        public GpServerInfo build() {
            return new GpServerInfo(this);
        }
    }

}
