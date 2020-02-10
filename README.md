# This project has been abandoned
As you might have noticed from the commit history, this project hasn't received the love it requires to keep it in working order. As maintainers have moved on to other projects and/or don't have the time to spend on it anymore, we decided to formally abandon this project. We'll keep it archived in Github so anyone willing to fork it is able to do so. Please note the permissions and limitations of the Apache license (see LICENSE file). Thanks!

# <img src="https://cloud.githubusercontent.com/assets/205326/8749163/038588f4-2ca0-11e5-94f7-25074b6b1ee8.png" alt="VisualReview logo" height="40x"> 
[![Build Status](https://travis-ci.org/xebia/VisualReview.svg?branch=master)](https://travis-ci.org/xebia/VisualReview) [![Join the chat at https://gitter.im/xebia/VisualReview](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/xebia/VisualReview?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

VisualReview's goal is to provide a productive and human-friendly workflow for testing and reviewing your web application's layout for any regressions.

VisualReview functions as a server which accepts screenshots of your web application, sent for example, from your selenium or protractor scripts.
These shots are then compared to screenshots you uploaded and reviewed earlier. VisualReview will display and highlight any differences
between the two and allows you to either accept or reject these changes.

Currently VisualReview provides a [Protractor plug-in](https://www.github.com/xebia/VisualReview-protractor) to easily
upload screenshots from your protractor tests. However, the VisualReview server has a simple API which allows
compatibility with other testing frameworks. We are working on other plug-ins and hope to release them soon.

## See it in action
![VisualReview visual regression tool in action](https://cloud.githubusercontent.com/assets/205326/8633297/1d86b464-27c1-11e5-85db-66cefbff4def.gif)

Above you can see how the application helps you to identify and evaluate visual changes in your application.

## Getting started
For a **quick demo** try the [protractor example](https://github.com/xebia/VisualReview-protractor/blob/master/example-project/README.md).

To use VisualReview we'll start the VisualReview app itself. After that, we'll run tests that send screenshots to the server. 

### Configuring and starting the VisualReview server

* Download and extract the [latest release](https://github.com/xebia/VisualReview/releases) or [build it and run from source](https://github.com/xebia/VisualReview/wiki/Building-and-running-from-source).
* Reconfigure any settings in config.edn (optional)
* Run `./start.sh`
* Open your browser at [http://localhost:7000](http://localhost:7000) (or the port you configured in config.edn) to see if everything is working. It should list an empty project list.

### Running your first test
* Send screenshots during a test to VisualReview. We currently provide a [Protractor plug-in](https://www.github.com/xebia/VisualReview-protractor) to do this. See
the protractor-plugin's [README](https://github.com/xebia/VisualReview-protractor/blob/master/README.md) for details on how to configure Protractor to send screenshots to VisualReview.

### Reviewing the results
* Go back to the VisualReview page on http://localhost:7000 (or the port you configured in config.edn).
* Navigate to your project and suite name. Here you see all the times you ran a test script against this project and suite combination.
* Click on the run to review all screenshots and differences. If this is the first time you created a run in this suite, there will be no differences yet.
* To approve or reject a screenshot, use the top-right menu or hit the 'a' or 'x' key on your keyboard.
  * When you accept a screenshot, it will be added to this suite's so-called 'baseline'. Every future screenshot with that name inside the suite will be compared to this baseline.
 All screenshots you accept in future runs will overwrite this baseline. The baseline will therefore contain all latest accepted screenshots of a suite to which all new screenshots will be compared against.
  * When you reject a screenshot, the baseline will not be updated.
  * After you either accepted or rejected a screenshot, you can always revert this decision by selecting the 'pending' option in the top right menu.

## Integration with other tools
An updated list of currently available integrations with other tools can be found [here](https://github.com/xebia/VisualReview/blob/master/doc/api.md#usages)

## How to contribute
VisualReview provides a REST API for easy integration with your own toolset. See the [API documentation](https://github.com/xebia/VisualReview/blob/master/doc/api.md) for more details.

To run or build the VisualReview project itself from source, see [this wiki page](https://github.com/xebia/VisualReview/wiki/Building-and-running-from-source).

## Features in development 
For our current- and future development focus, see the [wiki](https://github.com/xebia/VisualReview/wiki/Milestones). 

## Original authors and maintainers
We'd like to acknowledge the original authors of this project, from before version 0.1 and beyond:

* [Joshua Appelman](https://github.com/jbnicolai)
* [Daniel Marjenburgh](https://github.com/dmarjenburgh)
* [Sannie Kwakman](https://github.com/skwakman)
* [Ruurd Moelker](https://github.com/rrmoelker)

## License

Copyright © 2015 [Xebia](https://xebia.com/)

Distributed under the [Apache License 2.0](http://http://www.apache.org/licenses/LICENSE-2.0).


