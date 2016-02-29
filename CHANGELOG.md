# Changelog

## 0.1.3

### Features and improvements
* Scheduled cleanup now logs a message at start and finish. This enables users to validate their schedule configuration.

### Bugfixes
* Errors during cleanup schedule no longer fail without logging an error.
* Fixes bug where runs are no longer cleaned up when there are multiple projects defined in VisualReview

## 0.1.2

### Features and improvements
* Added option to enable logging of all incoming and outgoing HTTP traffic from/to VisualReview server. This eases development
 of other tools integrating with VisualReview.
* Added option to set a maximum amount of runs inside a suite. When a new run is added to a suite that has exceeded the amount of runs, the oldest run inside that suite is marked for deletion.
* Added cleanup scheduler for deleting unused images from deleted runs and deleting runs that exceed the maximum-amount-per-suite (if configured).
* Replaced favicon with new logo

### Bugfixes
* Fixed an issue where baseline images from deleted runs were no longer displayed in the GUI ([#52](https://github.com/xebia/VisualReview/issues/52)).

## 0.1.1

### Features and improvements
* Projects are now created on the fly during a test run, instead of having to create them manually beforehand. Thanks [wietsevennema](https://github.com/wietsevenema) !
* Added delete buttons for suites and links
* Breadcrumb navigation now disables link to the currently viewed page for increased clarity.
* Page title now contains more information on what's being shown on the page, instead of just 'VisualReview'.
* Various user interface tweaks

### Bugfixes
* Fixed an issue where suites could no longer be added to a project when the user already tried to run a test against a non-existing project. ([#39](https://github.com/xebia/VisualReview/issues/39))


## 0.1
Initial release


