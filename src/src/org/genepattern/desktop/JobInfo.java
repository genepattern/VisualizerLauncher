package org.genepattern.desktop;

import java.util.Map;

/**
 * Created by nazaire on 4/6/16.
 */
public class JobInfo
{
    private String jobNumber;
    private GPTask gpTask;
    private String[] commandLine;
    private Map inputURLToFilePathMap;

    public String getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(String jobNumber) {
        this.jobNumber = jobNumber;
    }

    public GPTask getGpTask() {
        return gpTask;
    }

    public void setGpTask(GPTask gpTask) {
        this.gpTask = gpTask;
    }

    public String[] getCommandLine() {
        return commandLine;
    }

    public void setCommandLine(String[] commandLine) {
        this.commandLine = commandLine;
    }

    public Map<String, String> getInputURLToFilePathMap() {
        return inputURLToFilePathMap;
    }

    public void setInputURLToFilePathMap(Map inputURLToFilePathMap) {
        this.inputURLToFilePathMap = inputURLToFilePathMap;
    }
}
