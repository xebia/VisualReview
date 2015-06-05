'use strict';

angular.module('visualDiffViewerApp')
  .factory('SuiteResource', function ($resource) {
    return $resource('/api/projects/:projectId/suites/:suiteId', {}, {
     remove: {
       method: 'DELETE'
     }
    });

  });
