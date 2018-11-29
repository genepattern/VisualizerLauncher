package org.genepattern.desktop;

import static org.genepattern.desktop.TestAll.TEST_JOB_ID;
import static org.genepattern.desktop.TestAll.TEST_USER;
import static org.genepattern.desktop.TestAll.gpServerInfo;
import static org.genepattern.desktop.GpServerInfo.GP_URL_DEFAULT;
import static org.genepattern.desktop.TestAll.JOBS_DIR;
import static org.genepattern.desktop.TestAll.TEST_DIR;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

public class TestJobInfo {
    private JobInfo jobInfo;
    
    @Before
    public void setUp() {
        jobInfo=new JobInfo(TEST_JOB_ID, JOBS_DIR);
    }

    protected GpServerInfo.Builder builderForTest() {
        return new GpServerInfo.Builder()
            .gpServer(GP_URL_DEFAULT)
            .user(TEST_USER)
            .jobNumber(TEST_JOB_ID);
    }

    @Test
    public void initAppDir() {
        // when property is not set ... 
    }

    @Test
    public void initLocalJobDir() {
        assertEquals("localJobDir, appDir=null",
                //expected
                new File("jobs/"+TEST_JOB_ID),
                //actual
                builderForTest().appDir(null).build().getLocalJobDir());

        assertEquals("localJobDir, appDir='.'",
                //expected
                new File("jobs/"+TEST_JOB_ID),
                //actual
                builderForTest().appDir(new File(".")).build().getLocalJobDir());

        assertEquals("localJobDir, appDir='./'",
                //expected
                new File("jobs/"+TEST_JOB_ID),
                //actual
                builderForTest().appDir(new File("./")).build().getLocalJobDir());

        assertEquals("localJobDir, appDir=''",
                //expected
                new File("jobs/"+TEST_JOB_ID),
                //actual
                builderForTest().appDir(new File("")).build().getLocalJobDir());

        assertEquals("localJobDir, appDir='appdata'",
                //expected
                new File("appdata/jobs/"+TEST_JOB_ID),
                //actual
                builderForTest().appDir(new File("appdata")).build().getLocalJobDir());

        assertEquals("localJobDir, appDir='./appdata'",
                //expected
                new File("appdata/jobs/"+TEST_JOB_ID),
                //actual
                builderForTest().appDir(new File("./appdata")).build().getLocalJobDir());

        assertEquals("initLocalJobDir",
            //expected
            new File(TEST_DIR, "jobs/"+TEST_JOB_ID),
            //actual
            builderForTest().appDir(TEST_DIR).build().getLocalJobDir());
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

    // special-case: null string
    @Test
    public void addInputFile_null() {
        jobInfo.addInputFile(gpServerInfo, (String)null);
        assertEquals("skipping inputFile=(null)", 0,  jobInfo.getInputFiles().size());
    }

}
