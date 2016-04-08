package org.genepattern.desktop;

/**
 * Created by nazaire on 4/6/16.
 */
public class GPTask
{
    private String lsid;
    private String[] supportFileUrls;
    private String commandLine;

    public String getLsid() {
        return lsid;
    }

    public void setLsid(String lsid) {
        this.lsid = lsid;
    }

    public String[] getSupportFileUrls() {
        return supportFileUrls;
    }

    public void setSupportFileUrls(String[] supportFileUrls) {
        this.supportFileUrls = supportFileUrls;
    }

    public String getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String commandLine) {
        this.commandLine = commandLine;
    }
}
