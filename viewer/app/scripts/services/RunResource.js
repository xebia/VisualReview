'use strict';

angular.module('visualDiffViewerApp')
  .factory('RunResource', function ($resource) {
    return $resource('/api/runs/:runId', {}, {
      'getScreenshots': {
        method: 'GET',
        url: '/api/runs/:runId/screenshots',
        isArray: true
      },
      'getAnalysis': {
        method: 'GET',
        url: '/api/runs/:runId/analysis'
      },
      'updateStatus': {
        method: 'POST',
        url: '/api/runs/:runId/analysis/diffs/:diffId'
      }
    });

  });
