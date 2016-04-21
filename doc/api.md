# API doc 

A REST API is available to send screenshots to the VisualReview server from another process.


## General structure

All calls to the API use the base url:

```
http://' + {{hostname}} + ':' + {{port}} + '/api/' + {{path}}
```

By default you should use hostname **localhost** and port **7000**.
The path depends on the end point.
Generally there an endpoint supports a GET, POST and DELETE method.

## End points

The API end points allow you to read and manipulate projects, suites, runs and the images within a run.
Here is a non-exhaustive list of the end points and available methods.

### Projects

Path: `projects`
Methods: GET

Retrieve list of projects.

Path: `projects/{{project_id}}`
Methods: GET, DELETE

Retrieve specific project.

***

### Suites

Path: `projects/{{project_id}}/suites/{{suite_id}}`
Methods: GET, DELETE

Retrieve and delete specific project.

#### Suite status

Path: `projects/{{project_id}}/suites/{{suite_id}}/status`
Methods: GET

Get last run result. This is useful to poll whether a suite is working as expected. Possible values:

* accepted
* rejected
* pending

### Runs

Path: `runs/{{run_id}}`
Methods: GET

Get run specific information such as *startTime* and *branchName*.

Path: `runs`
Methods: POST

Create a new run for for `{{suiteName}}` in `{{projectName}}`. The returned JSON object contains the run *id*

### Screenshots

Path: `runs/{{run_id}}/screenshots`
Methods: POST

Upload a screenshot to the supplied run.
The request should be just like a multi-part form post, like you're uploading a screenshot from a form.
Multipart elements should be:
 * **file** (binary, with the screenshot png data)
 * **screenshotName** (plain text, containing the screenshot name)
 * **properties**  (application/json, JSON object with image properties, see below)
 * **meta** (application/json, JSON object with additional data about the screenshot. Can be empty).

When a screenshot is uploaded it's compared to a previously uploaded screenshot having the same `screenshotName` and `properties`. The `properties` object has no predefined set of keys, however we suggest including at least a screen resolution and browser as these fields will be displayed by VisualReview's GUI. For example:
```javascript
properties = {
  resolution: size.width + 'x' + size.height,
  browser: 'firefox'
}
```

The field `metadata` can contain some additional data of the screenshot. Currently it's not being used by VisualReview's GUI, but might be handy for other tools using VisualReview's API.

### Analysis
Path: `runs/{{run_id}}/analysis`
Methods: GET

Get analysis for a given run.
An analysis contains an array of **diffs** one for each screenshot taken.
Each of these diffs have an *id* to a before and after image as well as the difference image.

### Images

Path: `image/{{image_id}}`
Methods: GET

Get the screenshot or difference png image.

## HTTP logging
(since 0.1.2) VisualReview can log incoming API request- and outgoing response-data. This greatly eases development of new tools and libraries
integrating with VisualReview.

To enable HTTP logging, add the following to config.edn:
`:enable-http-logging true`

## Currently known integrations with VisualReview

The following parts/project already make use of these API calls:

* VisualReview/viewer web app, mostly reads projects, suites, runs and screenshots for reviewing purpose. See the Angular `...Resource.js` files
* [VisualReview-protractor](https://github.com/xebia/VisualReview-protractor), create new runs and screenshots.
* [VisualReview-node-client](https://github.com/Klaasvaak/VisualReview-node-client), Node.js client for VisualReview
* [VisualReview-selenium-java-prototype](https://github.com/skwakman/visualreview-selenium-java-prototype), Java implementation that can create suites, runs and ofcourse send screenshots. Very quickly setup for demonstration purposes.
* [VisualReview-docker](https://github.com/FinKingma/VisualReviewDockerfile), Dockerfile to run VisualReview.
