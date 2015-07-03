'use strict';

angular.module('visualDiffViewerApp')
  .factory('MathService', function () {
    return {
      clamp: function (value, min, max) {
        return Math.min(Math.max(value, min), max);
      }
    };
  });
