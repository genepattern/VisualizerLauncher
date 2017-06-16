package org.genepattern.desktop;

import static org.genepattern.desktop.TestAll.GP_URL;
import static org.genepattern.desktop.TestAll.JOBS_DIR;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

/**
 * Test cases for JobInfo prepareCommandLine
 * 
 * @author pcarr
 */
public class TestJobInfo_commandline {

    static final String jobId="1";
    static final File jobdir=new File(JOBS_DIR, jobId);
    static final String user="test";
    
    private GpServerInfo info;
    private JobInfo jobInfo;
    // default inputFile url
    private String inputFile=GP_URL+"/jobResults/1/all_aml_test.gct";
    // default local file path
    private String localPath=new File(jobdir, "all_aml_test.gct").getAbsolutePath();

    @Before
    public void setUp() {
        info=new GpServerInfo.Builder()
            .gpServer(GP_URL)
            .user(user)
            .jobNumber(jobId)
        .build();
        jobInfo=new JobInfo(jobId, jobdir);
    }

    @Test
    public void inputFile_asArg() {
        jobInfo.addInputFile(info, inputFile);
        //String[] cmdIn={ "java", "-c"+inputFile };
        String[] cmdIn={ "java", inputFile };
        String[] cmdOut=jobInfo.substituteLocalFilePaths(info, cmdIn);
        assertEquals(localPath, cmdOut[1]);
    }

    @Test
    public void inputFile_asArg_special_char() {
        inputFile=GP_URL+"/jobResults/1/all%20aml%20test.gct";
        localPath=new File(jobdir, "all aml test.gct").getAbsolutePath();
        jobInfo.addInputFile(info, inputFile);
        String[] cmdIn={ "java", inputFile };
        String[] cmdOut=jobInfo.substituteLocalFilePaths(info, cmdIn);
        assertEquals(localPath, cmdOut[1]);
    }
    
    // test 2: '-c<inputFile>' 
    @Test
    public void arg_with_flag() {
        JobInfo jobInfo=new JobInfo(jobId, jobdir);
        jobInfo.addInputFile(info, inputFile);
        final String[] cmdIn={ "java", "-c"+inputFile };
        String[] cmdOut=jobInfo.substituteLocalFilePaths(info, cmdIn);
        assertEquals("-c"+localPath, cmdOut[1]);
    }

}
