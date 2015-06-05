# VisualReview [![Build Status](https://travis-ci.org/xebia/VisualReview.svg?branch=master)](https://travis-ci.org/xebia/VisualReview)

VisualReview's goal is to provide a productive and human-friendly workflow for testing and reviewing your web application's layout
across several browsers, resolutions and platforms.

VisualReview functions as a server which accepts screenshots of your web application, sent for example, from your selenium or protractor scripts.
These shots are then compared to screenshots you uploaded and reviewed earlier. VisualReview will display and highlight any differences
between the two and allows you to either accept or reject these changes.

Currently VisualReview provides a [Protractor plug-in](https://www.github.com/xebia/VisualReview-protractor) to easily
upload screenshots from your protractor tests. However, the VisualReview server has a simple API which allows
compatibility with other testing frameworks. We are working on other plug-ins and hope to release them soon.

## Getting started

### Configuring and starting the VisualReview server

* Download and extract the latest release from [here](https://github.com/xebia/VisualReview/releases).
* Reconfigure any settings in config.edn (optional)
* Make sure that the screenshots directory exists and is readable (default is ```screenshots```)
* Run ./start.sh
* Open your browser at [http://localhost:7000](http://localhost:7000) (or the port you configured in config.edn)
* Create a new project

### Running your first test
* Send screenshots during a test to VisualReview. We currently provide a [Protractor plug-in](https://www.github.com/xebia/VisualReview-protractor) to do this. See
the protractor-plugin's [README](https://github.com/xebia/VisualReview-protractor/blob/master/README.md) for details on how to configure Protractor to send screenshots to VisualReview.

### Reviewing the results
* Go back to the VisualReview page on http://localhost:7000 (or the port you configured in start.sh).
* Navigate to your project and suite name. Here you see all the times you ran a test script against this project and suite combination.
* Click on the run to review all screenshots and differences. If this is the first time you created a run in this suite, there will be no differences yet.
* To approve or reject a screenshot, use the top-right menu or hit the 'a' or 'x' key on your keyboard.
  * When you accept a screenshot, it will be added to this suite's so-called 'baseline'. Every future screenshot with that name inside the suite will be compared to this baseline.
 All screenshots you accept in future runs will overwrite this baseline. The baseline will therefore contain all latest accepted screenshots of a suite to which all new screenshots will be compared against.
  * When you reject a screenshot, the baseline will not be updated.
  * After you either accepted or rejected a screenshot, you can always revert this decision by selecting the 'pending' option in the top right menu.


## Features in development (aka known issues, limitations)
VisualReview is currently in heavy development and is not yet ready for production use.
Here's a (not finalized) list of features we'd like to get finished before moving towards a stable 1.0.0 release.

* Baseline management GUI
* Add GUI for deleting suites and runs
* Provide screenshot cleanup functionality to save disk space.
* Add image zoom tools in GUI
* Add options for adding VisualReview as part of a pull-request workflow (slated for v0.2)

## VisualReview Enterprise

In addition we are also working on an Enterprise edition of VisualReview.
This edition will contain all the features of the open source edition and adds enterprise features such as user authentication,
access from the cloud, enterprise support and more. We will release more details in the future. In the meantime, you can contact us
at [visualreview@xebia.com](mailto:visualreview@xebia.com) to receive updates.

## How to contribute
To run or build the project from source, see [this wiki page](https://github.com/xebia/VisualReview/wiki/Building-and-running-from-source).

## Original authors and maintainers
We'd like to acknowledge the original authors of this project, from before version 0.1 and beyond:

* [Joshua Appelman](https://github.com/jbnicolai)
* [Daniel Marjenburgh](https://github.com/dmarjenburgh)
* [Sannie Kwakman](https://github.com/skwakman)

## License

Copyright © 2015 [Xebia](https://xebia.com/)

Distributed under the [Apache License 2.0](http://http://www.apache.org/licenses/LICENSE-2.0).


