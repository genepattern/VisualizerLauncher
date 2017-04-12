## Usage
     # run visualizer launcher (for debugging, testing)
      mvn -q clean package exec:java
      # create single jar application
      mvn clean compile assembly:single 
      # create Mac OS X application bundle
      mvn package appbundle:bundle
      
      # run visualizer launcher with the java command
      java -jar launcher/${jarName}-full.jar
      
      
      # use a different certificate ...
      mvn -Dsignjar.keystore=${HOME}/.gp_build/genepattern-codesign.jks
        -Dsignjar.alias=codesign
        -Dsignjar.storepass=....
        


## Building from source
Use **mvn** to build the project with the defaultGoal "package" 

    mvn
    (equivalently) mvn package
    
This creates packages in the ./target directory.
* (java executable) visualizerLauncher-{version}-full.jar 
* (Mac OS X app)    visualizerLauncher-{version}/VisualizerLauncher.app

To run as a jar executable

    mvn -q clean package exec:java

For more details ... consult the pom.xml file 

    mvn --help
    mvn help:help -Ddetail=true

## VisualizerLauncher
Launches a GenePattern visualizer from the desktop. Use this as a replacement for the 'Open Visualizer' link embedded in the web page. 

#### To get started ...
The application is available for download directly from the [releases](https://github.com/genepattern/VisualizerLauncher/releases/latest) page. 

**Mac OS X:** Download the native .app.zip file; Unzip and double-click the app.

**Other java:** Download the jar file. From a Terminal window

    java -jar visualizerLauncher.jar
    
#### Launching your visualizer ...
The VisualizerLauncher opens in a new application window. If for some reason it is hidden, look for the "VisualizerLauncher" java application icon in your dock. Enter the server, (e.g. http(s)://genepattern.broadinstitute.org/gp), your username, password, and the job number of your visualizer job. Click 'Submit'. The launcher will connect to the server, download the required application and data files, and launch the visualizer as a new window.

#### Building from source
