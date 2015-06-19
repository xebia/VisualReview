'use strict';

angular.module('visualDiffViewerApp')
  .factory('humanejs', function ($window) {
    return $window.humane;
});
