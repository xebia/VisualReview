'use strict';

angular.module('visualDiffViewerApp')
  .factory('hamsterjs', function ($window) {
    return $window.Hamster;
  });
