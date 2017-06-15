package org.genepattern.desktop;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestAppDirUtil {
    String userDataDir_initial=null;
    
    @Before
    public void setUp() throws IOException {
        userDataDir_initial=System.getProperty(AppDirUtil.PROP_USER_DATA_DIR);
        if (userDataDir != null) {
            System.setProperty(AppDirUtil.PROP_USER_DATA_DIR, userDataDir);
        }
    }
    
    @After
    public void tearDown() {
        if (userDataDir_initial != null) {
            System.setProperty(AppDirUtil.PROP_USER_DATA_DIR, userDataDir_initial);
        }
        else {
            System.clearProperty(AppDirUtil.PROP_USER_DATA_DIR);
        }
    }

    @Parameters(name="-Duser.data.dir=\"{0}\"")
    public static Collection<Object[]> data() throws IOException {
        final File cwd=new File(".").getCanonicalFile();
        final File home=new File(System.getProperty("user.home")).getCanonicalFile();
        return Arrays.asList(new Object[][] {
            { null, AppDirUtil.getAppDir_standard() },
            { "", cwd },
            { " ", cwd },
            { ".", cwd },
            { "./", cwd },
            { "~", home },
            { "~/", home },
            { "~/.visualizerLauncher", new File(home,".visualizerLauncher") },
        });
    }
    
    @Parameter(0)
    public String userDataDir;
    
    @Parameter(1)
    public File expectedFile;
    
    @Test
    public void getAppDir() {
        assertEquals("getAppDir()", 
            expectedFile, 
            AppDirUtil.getAppDir());
    }

    @Test
    public void initUserDataDir() {
        //special-case, skip this test when userDataDir==null
        if (userDataDir==null) {
            return;
        }
        assertEquals("'user.data.dir'='"+userDataDir+"'",
            expectedFile,
            AppDirUtil.initUserDataDir_from_sys(userDataDir));
    }

}
