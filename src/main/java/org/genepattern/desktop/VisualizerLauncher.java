package org.genepattern.desktop;

public class VisualizerLauncher {
    public static void main(String[] args) {
        LogUtil.initLogging();
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                VisualizerLauncherGui.createAndShowGUI();
            }
        });
    }
}
