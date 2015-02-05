'use strict';

angular.module('visualDiffViewerApp')
  .directive('navbar', function() {
    return {
      restrict: 'A',
      scope: {
        'projectId': '=',
        'projectName': '=',
        'suiteId': '=',
        'suiteName': '=',
        'runId': '=',
        'screenshot': '=',
        'screenshots': '=',
        'screenshotType': '=',
        'showDiff': '=',
        'creationTime': '='
      },
      templateUrl: 'views/navbar.html',
      link: function(scope) {
        scope.toggleStatus = function(newStatus) {
          scope.screenshot.status = newStatus;
        };

        scope.selectScreenshot = function(index) {
          scope.$emit('vr-screenshot-select',index)
        }

      }
    };
  });
