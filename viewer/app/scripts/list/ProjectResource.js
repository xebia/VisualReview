'use strict';

angular.module('visualDiffViewerApp')
  .factory('ProjectResource', function ($resource) {
    return $resource('/api/projects', {}, {
        list: {method:'GET', isArray: true},
        getById: {
          method: 'GET', url: '/api/projects/:projectId'
        },
        create: {
          method: 'PUT'
        },
        remove: {
          method: 'DELETE', url: '/api/projects/:projectId'
        }
      }
    );
  });
