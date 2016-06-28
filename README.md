## VisualizerLauncher
Launches a GenePattern visualizer from the desktop. Use this as a replacement for the 'Open Visualizer' link embedded in the web page. 

#### To get started ...
**Mac OS X:** Download the native .app.zip file; Unzip and double-click the app.

**Other java:** Download the jar file. From a Terminal window

    java -jar visualizerLauncher.jar
    
#### Launching your visualizer ...
The VisualizerLauncher opens in a new application window. If for some reason it is hidden, look for the "VisualizerLauncher" java application icon in your dock. Enter the server, (e.g. http(s)://genepattern.broadinstitute.org/gp), your username, password, and the job number of your visualizer job. Click 'Submit'. The launcher will connect to the server, download the required application and data files, and launch the visualizer as a new window.

#### Building from source
Use **ant** to build the project with the default "package" target.

    ant 
    
This will create the runVisualizer.jar file in the ./dist directory. For more details ...

    ant -p


