package org.genepattern.desktop;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;


/**
 * Test cases for visualizerInputFile paths from REST API,
 *   GET /gp/rest/v1/jobs/{jobId}/visualizerInputFiles
 * Example json response:
<pre>
 { "inputFiles": [
     "https://genepattern.broadinstitute.org/gp/jobResults/1/all_aml_test.gct",
     "<GenePatternURL>gp/jobResults/1/all%20aml%20test.gct",
     "/gp/jobResults/1/all%20aml%20test.gct"
 ]
 }
</pre>
 *
 * Parameterized tests for 
 *   {@link JobInfo#initInputFileUrlStr(GpServerInfo, String)}
 *   and 
 *   {@link JobInfo#extractFilenameFromUrl(String)}
 * 
 * @author pcarr
 */
@RunWith(Parameterized.class)
public class TestJobUtil_filepaths {
    
    // gpServer variations
    public static final String[] gpUrls = {
        "https://genepattern.broadinstitute.org/gp", 
        "http://127.0.0.1:8080/gp"
    };

    // username variations, { username, urlEncoded(username) }
    public static final String[][] users= {
        { "test", "test" },
        { "test@example.com", "test%40example.com" },
        { "test user", "test%20user" }
    };

    // filename variations, { filepath, urlEncoded(filepath) }
    public static final String[][] filepaths= {
            { "all_aml_test.gct", "all_aml_test.gct" },
            { "all aml test.gct", "all%20aml%20test.gct" },
    };
    
    // urlpath variations
    public static final String[] initUrlPaths(final String user, final String filepath) {
        return new String[] {
            // job upload file
            "users/" + user + "/tmp/run123456/input.filename/1/"+filepath,
            // previous job result file
            "jobResults/1/"+filepath,
        };
    }

    // inputFile link variations
    public static final String[] initLinks(final String gpUrl) {
        return new String[] {
            // special-case: missing gpUrl
            "/gp/",
            // special-case: <GenePatternURL> substitution
            "<GenePatternURL>",
            // special-case: <GenePatternURL> substitution with trailing slash
            "<GenePatternURL>/",
            gpUrl + "/",
        };
    }

    @Parameters(name="inputFile={2}")
    public static Collection<Object[]> data() {
        final List<Object[]> testCases=new ArrayList<Object[]>();
        for(final String gpUrl : gpUrls) {
            final String[] links = initLinks(gpUrl);
            for(final String[] user : users) {
                for(final String[] filepath : filepaths) { 
                    final String[] urlPaths = initUrlPaths(user[1], filepath[1]);
                    for(final String link : links) {
                        for(final String urlPath : urlPaths) {
                            final String inputFile=link + urlPath;
                            final String expectedUrl = gpUrl + "/" + urlPath;
                            final String expectedFilepath = filepath[0];
                            testCases.add(new String[] { gpUrl, user[0], inputFile, expectedUrl, expectedFilepath });
                        }
                    }
                }
            }
        }
        return testCases;
    }

    @Parameter(0)
    public String gpServer;
    
    @Parameter(1)
    public String user;
    
    @Parameter(2)
    public String inputFile="";

    @Parameter(3)
    public String expectedUrl="";
    
    @Parameter(4)
    public String expectedFilepath="";

    @Test
    public void initInputFileUrlStr() {
        final GpServerInfo info=new GpServerInfo.Builder()
            .gpServer(gpServer)
            .user(user)
        .build();
        assertEquals(
                // expected
                expectedUrl, 
                // actual
                JobInfo.initInputFileUrlStr(info, inputFile));
    }

    @Test
    public void extractFilenameFromUrl() {
        assertEquals(
            //expected
            expectedFilepath,
            // actual
            JobInfo.extractFilenameFromUrl(expectedUrl));
    }

}
