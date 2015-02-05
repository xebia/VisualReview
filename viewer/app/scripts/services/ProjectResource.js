'use strict';

angular.module('visualDiffViewerApp')
  .factory('ProjectResource', function ($resource) {
    return $resource('/api/projects', {}, {
        list: {method:'GET', isArray: true},
        getById: {
          method: 'GET', url: '/api/projects/:projectId'
        },
        create: {
          method: 'PUT',
          headers : {'Content-Type': 'application/x-www-form-urlencoded'},
          transformRequest: function(parameterObj) {
            // transform to form data
            var formParams = [];
            for(var p in parameterObj)
              formParams.push(encodeURIComponent(p) + "=" + encodeURIComponent(parameterObj[p]));
            return formParams.join("&");
          }
        }
      }
    );
  });
