'use strict';

angular.module('visualDiffViewerApp')
  .constant('KEY_CODES', {
    37: 'LEFT',
    38: 'UP',
    39: 'RIGHT',
    40: 'DOWN',
    65: 'A',
    68: 'D',
    88: 'X'
  })
  .directive('keyboard', function ($document, $rootScope, KEY_CODES) {
    return {
      restrict: 'A',
      scope: false,
      link: function ($scope, element) {
        $document.bind('keydown', function (e) {
          var key = KEY_CODES[e.which || e.keyCode];
          if (key) {
            e.preventDefault();
            $rootScope.$broadcast('keydown', key);
          }
        });

        element.on('$destroy', function () {
          $document.unbind('keydown');
        })
      }
    };
  });
