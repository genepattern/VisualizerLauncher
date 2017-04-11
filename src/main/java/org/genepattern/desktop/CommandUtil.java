package org.genepattern.desktop;

import java.io.IOException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;

public class CommandUtil {
    
    public static void runCommand(final String[] command) throws ExecuteException, IOException {
        Executor executor = new DefaultExecutor();
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
        CommandLine cmdLine=new CommandLine(command[0]);
        for(int i=1; i<command.length; ++i) {
            cmdLine.addArgument(command[i]);
        }
        executor.execute(cmdLine, resultHandler);
    }

}
