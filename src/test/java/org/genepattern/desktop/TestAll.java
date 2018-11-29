package org.genepattern.desktop;

import static org.genepattern.desktop.GpServerInfo.GP_URL_DEFAULT;
import static org.junit.Assert.*;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Start here to implement test suite for the VisualizerLauncher.
 * 
 * Presently, some tests require a completed visualizer job on a publicly available
 * GP server. They are useful while developing but will not work out of the box. 
 * 
 * @author pcarr
 *
 */
public class TestAll {
    private static final Logger log = LogManager.getLogger(TestAll.class);
    
    public static final String TEST_USER="test_user";
    public static final String TEST_JOB_ID="1";

    // temp directory for testing
    public static final File TEST_DIR=new File("test/tmp");
    public static final File JOBS_DIR=new File(TEST_DIR, "jobs");
    
    public static final GpServerInfo gpServerInfo=initGpServerInfo();

    public static GpServerInfo initGpServerInfo() {
        return new GpServerInfo.Builder()
                .gpServer(GP_URL_DEFAULT)
                .user(TEST_USER)
                .jobNumber(TEST_JOB_ID)
            .build();
    }
    
    public static final void assertStartsWith(final String message, final String expected, final String actual) {
        if (!actual.startsWith(expected)) {
            fail(message+": !startsWith('"+expected+"'), actual="+actual);
        }
    }

    @Test
    public void test() {
        log.debug("testing");
        assertEquals("true", "true", "true");
    }
    
    @Test
    public void initBasicAuthHeader() {
        assertEquals("basic test", "Basic dGVzdDp0ZXN0",
            Util.initBasicAuthHeader("test", "test"));
        assertEquals("basic test (null password)", "Basic dGVzdDo=",
            Util.initBasicAuthHeader("test", null));
        assertEquals("basic test (empty password)", "Basic dGVzdDo=",
            Util.initBasicAuthHeader("test", ""));
    }
    
    /**
     * TODO: this test requires a valid visualizer job id.
     * To setup, run a visualizer on the GP server and take 
     * note of the jobId.
     */
    @Ignore @Test
    public void testRetrieveJobDetails() { //throws Exception {
        final String username="test_user";
        final String password="test_password";
        final String jobId="1472248";
        // PredictionResultsViewer_v1 lsid
        final String PRViewer_LSID=
            "urn:lsid:broad.mit.edu:cancer.software.genepattern.module.visualizer:00019:1";

        final String basicAuthHeader=Util.initBasicAuthHeader(username, password);
        String actual="";
        try {
            actual=JobInfo.retrieveJobDetails(basicAuthHeader, GP_URL_DEFAULT, jobId);
        }
        catch (Exception e) {
            fail(""+e.getMessage());
        }
        assertEquals("expectedLsid", PRViewer_LSID, 
            actual);
    }
}
