## GenePattern Visualizer Launcher

Launch a GenePattern visualizer from the desktop. Use this instead of the 'Open Visualizer' link embedded in the web page. This [blog post](http://software.broadinstitute.org/cancer/software/genepattern/blog/2017/03/16/java-applet-based-visualizers-no-longer-function-in-any-browser) has more details.

### To get started ... 
**\[Mac OS X\]** Download [VisualizerLauncher.app.zip](https://github.com/genepattern/VisualizerLauncher/releases/download/v1.2.3/VisualizerLauncher.app.zip), unzip, and open the app.
**\[Other java\]** Download [visualizerLauncher.jar](https://github.com/genepattern/VisualizerLauncher/releases/download/v1.2.3/visualizerLauncher-1.2.3.jar) and double-click the jar file.

### Launching a visualizer ...
The VisualizerLauncher opens in a new application window. If for some reason it is hidden, look for the "VisualizerLauncher" java application icon in your dock. Enter ...
* server, e.g. `https://genepattern.broadinstitute.org/gp`
* _your genepattern username_
* _your genepattern password_
* _the job number of your visualizer job_

Click `Submit` ... the launcher downloads the application and data files from the server, then opens the visualizer in a new window.
<!--
Name | Value
------------: | :-------------
server | https://genepattern.broadinstitute.org/gp
username | _your genepattern username_
password | _your genepattern password_
job number | _the job number of your visualizer job_
-->

*Note:* To start from a command line shell (aka Terminal.app on Mac OS X) ...
```
java -jar visualizerLauncher-1.2.3.jar
```

### Support or Contact

Having trouble with the Visualizer Launcher? Check out [genepattern.org](https://genepattern.org/) or our [blog](http://software.broadinstitute.org/cancer/software/genepattern/blog).
