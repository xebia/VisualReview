'use strict';

angular.module('visualDiffViewerApp')
  .factory('mathjs', function ($window) {
    return $window.math;
  });
