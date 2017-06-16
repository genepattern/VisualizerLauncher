package org.genepattern.desktop;

import static org.genepattern.desktop.TestAll.TEST_JOB_ID;
import static org.genepattern.desktop.TestAll.gpServerInfo;
import static org.genepattern.desktop.TestAll.JOBS_DIR;
import static org.genepattern.desktop.TestAll.TEST_DIR;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;


public class TestJobInfo {
    private JobInfo jobInfo;
    
    @Before
    public void setUp() {
        jobInfo=new JobInfo(TEST_JOB_ID, JOBS_DIR);
    }
    
    @Test
    public void initLocalJobDir() {
        //tautological test
        assertEquals("initLocalJobDir",
            //expected
            new File(TEST_DIR, "jobs/"+TEST_JOB_ID),
            //actual
            JobInfo.initLocalJobDir(TEST_DIR, TEST_JOB_ID));
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
    
    // TODO: special-case: Windows file paths
    @Ignore @Test
    public void windows_test() {
        jobInfo.addInputFile(gpServerInfo, "/gp/jobResults/123456/all_aml_test.gct");
        InputFileInfo i=jobInfo.getInputFiles().get(0);
        
        assertEquals(
            // expected
            "-c"+jobInfo.getJobDir().getAbsolutePath()+File.separator+i.getFilename(),
            // actual
            i.replaceUrlWithLocalPath(jobInfo.getJobDir(), "-c"+i.getArg()));
    }

}
