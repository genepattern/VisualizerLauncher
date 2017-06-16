package org.genepattern.desktop;

import static org.genepattern.desktop.TestAll.TEST_JOB;
import static org.genepattern.desktop.TestAll.gpServerInfo;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;


public class TestJobUtil {
    private JobInfo jobInfo;
    
    @Before
    public void setUp() {
        jobInfo=new JobInfo(TEST_JOB);
    }
    
    @Test
    public void addInputFile() {
        final String inputFile=gpServerInfo.getGpServer()+"/jobResults/123456/all%20aml%20test.gct";
        jobInfo.addInputFile(gpServerInfo, inputFile);
        assertEquals("addInputFile('"+inputFile+"').url", 
                gpServerInfo.getGpServer()+"/jobResults/123456/all%20aml%20test.gct",  
                jobInfo.getInputFiles().get(0).getUrl());
        assertEquals("addInputFile('"+inputFile+"').arg", 
                gpServerInfo.getGpServer()+"/jobResults/123456/all%20aml%20test.gct",  
                jobInfo.getInputFiles().get(0).getArg());
        assertEquals("addInputFile('"+inputFile+"').filename", 
                "all aml test.gct",  
                jobInfo.getInputFiles().get(0).getFilename());
    }

    // special-case: '/gp/...'
    @Test
    public void addInputFile_prepend_gp() {
        final String inputFile="/gp/jobResults/123456/all%20aml%20test.gct";
        jobInfo.addInputFile(gpServerInfo, inputFile);
        assertEquals("addInputFile('"+inputFile+"').url", 
                gpServerInfo.getGpServer()+"/jobResults/123456/all%20aml%20test.gct",  
                jobInfo.getInputFiles().get(0).getUrl());
        assertEquals("addInputFile('"+inputFile+"').arg", 
                "/gp/jobResults/123456/all%20aml%20test.gct",  
                jobInfo.getInputFiles().get(0).getArg());
        assertEquals("addInputFile('"+inputFile+"').filename", 
                "all aml test.gct",  
                jobInfo.getInputFiles().get(0).getFilename());
    }

    // special-case: '<GenePatternURL>...'
    @Test
    public void addInputFile_substitute_GenePatternURL() {
        final String inputFile="<GenePatternURL>jobResults/123456/all%20aml%20test.gct";
        jobInfo.addInputFile(gpServerInfo, inputFile);
        assertEquals("addInputFile('"+inputFile+"').url", 
                gpServerInfo.getGpServer()+"/jobResults/123456/all%20aml%20test.gct",  
                jobInfo.getInputFiles().get(0).getUrl());
        assertEquals("addInputFile('"+inputFile+"').arg", 
                gpServerInfo.getGpServer()+"/jobResults/123456/all%20aml%20test.gct",  
                jobInfo.getInputFiles().get(0).getArg());
        assertEquals("addInputFile('"+inputFile+"').filename", 
                "all aml test.gct",  
                jobInfo.getInputFiles().get(0).getFilename());
    }

    // special-case: empty string
    @Test
    public void addInputFile_empty() {
        jobInfo.addInputFile(gpServerInfo, "");
        assertEquals("skipping inputFile=''", 0,  jobInfo.getInputFiles().size());
    }

    @Test
    public void addInputFile_null() {
        jobInfo.addInputFile(gpServerInfo, (String)null);
        assertEquals("skipping inputFile=(null)", 0,  jobInfo.getInputFiles().size());
    }

}
