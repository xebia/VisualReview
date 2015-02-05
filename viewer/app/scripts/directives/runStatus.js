'use strict';

angular.module('visualDiffViewerApp')
  .directive('runStatus', function(runStatusFilter) {
    return {
      restrict: 'A',
      scope: {
        runData: '=runStatus' // should contain an array of imageMetaData objects
      },
      templateUrl: 'views/run-status.html',
      link: function(scope) {
        var runStatus = runStatusFilter(scope.runData);

        scope.nrOfPending = runStatus.nrOfPending;
        scope.nrOfAccepted = runStatus.nrOfAccepted;
        scope.nrOfRejected = runStatus.nrOfRejected;

        angular.forEach(scope.runData, function(value, key) {
         switch(value.status) {
           case 'ACCEPTED':
             scope.nrOfAccepted++;
             break;

           case 'REJECTED':
             scope.nrOfRejected++;
             break;

           case 'PENDING':
             scope.nrOfPending++;
             break;
         }
        }, this);
      }
    };
  });
