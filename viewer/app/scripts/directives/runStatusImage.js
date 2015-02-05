'use strict';

angular.module('visualDiffViewerApp')
  .directive('runStatusImage', function(runStatusFilter) {
    return {
      restrict: 'A',
      scope: {
        runData: '=runStatusImage' // should contain an array of imageMetaData objects
      },
      template: '<div class="run-image float-left" ng-class="\'run-image-\' + status"></div>',
      link: function(scope) {
        var runStatus = runStatusFilter(scope.runData);

        if (runStatus.nrOfRejected > 0) {
          scope.status = 'rejected';
        } else if (runStatus.nrOfPending > 0) {
          scope.status = 'pending';
        } else {
          scope.status = 'accepted';
        }
      }
    };
  });
