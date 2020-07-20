## GenePattern Visualizer Launcher

Launch a GenePattern visualizer from the desktop. Use this instead of the 'Open Visualizer' link embedded in the web page. This [blog post](http://software.broadinstitute.org/cancer/software/genepattern/blog/2017/03/16/java-applet-based-visualizers-no-longer-function-in-any-browser) has more details.

### To get started ... 
**\[Mac OS X\]** Download [VisualizerLauncher.app.zip](https://github.com/genepattern/VisualizerLauncher/releases/download/v1.2.8/VisualizerLauncher.app.zip), unzip, and open the app.

**\[Other java\]** Download [visualizerLauncher.jar](https://github.com/genepattern/VisualizerLauncher/releases/download/v1.2.8/visualizerLauncher-1.2.8.jar) and double-click the jar file.

### Launching a visualizer ...
Run the visualizer of interest in GenePattern, as you normally would. Once the Job Status page displays, and the visualizer fails to launch in your browser, you will now have the _job number_ to provide to the app.

After opening the app (either by double clicking or opening - see above for OS specific instructions), the VisualizerLauncher opens in a new application window.
You will then need to enter the following information...
* server, e.g. `https://cloud.genepattern.org/gp` (this can be copied from the URL bar in your browser)
* _your genepattern username_
* _your genepattern password_
* _the job number of your visualizer job_

Click `Submit` ... the launcher downloads the application and data files from the server, then opens the visualizer in a new window.

*Note:* To start from a command line shell (aka Terminal.app on Mac OS X) ...
```
java -jar visualizerLauncher-1.2.8.jar
```

### Support or Contact

Having trouble with the Visualizer Launcher? Check out our [blog](http://software.broadinstitute.org/cancer/software/genepattern/blog/2017/03/16/java-applet-based-visualizers-no-longer-function-in-any-browser) or [contact us](http://software.broadinstitute.org/cancer/software/genepattern/contact)

Mac users - If you have trouble running the MacOS app (JRELoadError") try launching the .jar file from the command line. As always, feel free to [contact us](http://software.broadinstitute.org/cancer/software/genepattern/contact) with any questions.
