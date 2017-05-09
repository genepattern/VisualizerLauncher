package org.genepattern.desktop;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CommandUtil {
    private static final Logger log = LogManager.getLogger(CommandUtil.class);
    
    public static void runCommand(final String[] command) throws ExecuteException, IOException {
        final boolean handleQuoting=false;
        runCommand(command, handleQuoting);
    }

    public static void runCommand(final String[] command, final boolean handleQuoting) throws ExecuteException, IOException {
        Executor executor = new DefaultExecutor();
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        CommandLine cmdLine=new CommandLine(command[0]);
        for(int i=1; i<command.length; ++i) {
            cmdLine.addArgument(command[i], handleQuoting);
        }
        if (log.isDebugEnabled()) {
            log.debug("commons.exec.CommandLine.toString: " + cmdLine.toString());
        }
        executor.execute(cmdLine, resultHandler);
    }

}
