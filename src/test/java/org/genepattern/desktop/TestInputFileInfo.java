package org.genepattern.desktop;

import static org.genepattern.desktop.GpServerInfo.GP_URL_DEFAULT;
import static org.genepattern.desktop.TestAll.JOBS_DIR;
import static org.genepattern.desktop.TestAll.TEST_JOB_ID;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

public class TestInputFileInfo {
    final String inputFile=GP_URL_DEFAULT+"/jobResults/"+TEST_JOB_ID+"/all_aml_test.gct";
    final File jobDir=new File(JOBS_DIR, TEST_JOB_ID).getAbsoluteFile();
    final InputFileInfo inputFileInfo=new InputFileInfo(TestAll.gpServerInfo, inputFile);

    @Test
    public void toLocalPath() {
        assertEquals(
            // expected
            jobDir+File.separator+"all_aml_test.gct", 
            // actual
            inputFileInfo.toLocalPath(jobDir)
        ); 
    }

    @Test
    public void toLocalPath_relative() {
        final File jobDir=new File(JOBS_DIR, TEST_JOB_ID);
        assertEquals(
            // expected
            jobDir+File.separator+"all_aml_test.gct", 
            // actual
            inputFileInfo.toLocalPath(jobDir)
        ); 
    }

    @Test
    public void toLocalPath_null() {
        final File jobDir=null;
        assertEquals(
            // expected
            "all_aml_test.gct", 
            // actual
            inputFileInfo.toLocalPath(jobDir)
        ); 
    }

    @Test
    public void toLocalPath_empty() {
        final File jobDir=new File("");
        assertEquals(
            // expected
            "all_aml_test.gct", 
            // actual
            inputFileInfo.toLocalPath(jobDir)
        ); 
    }

    @Test
    public void substituteLocalPath_02() {
        assertEquals(
            "-c"+inputFileInfo.toLocalPath(jobDir), 
            inputFileInfo.substituteLocalPath("-c"+inputFileInfo.getArg(), jobDir));
    }

    @Test
    public void substituteLocalPath_01() {
        assertEquals(
            // expected
            "-c"+jobDir+File.separator+"all_aml_test.gct",
            // actual
            inputFileInfo.substituteLocalPath("-c"+inputFileInfo.getArg(), jobDir));
    }
    
    @Test
    public void replaceAll_windows_path() {
        final String example_local_path="C:\\Users\\test_user\\VisualizerLauncher\\test\\tmp\\jobs\\1\\all_aml_test.gct";
        final String argIn="-c"+inputFileInfo.getArg();
        assertEquals(
                // expected
                "-c"+example_local_path,
                //actual
                argIn.replaceAll(
                        Pattern.quote(inputFileInfo.getArg()), 
                        Matcher.quoteReplacement(example_local_path)));
    }
}
