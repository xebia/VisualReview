'use strict';

angular.module('visualDiffViewerApp')
  .directive('vrScreenshotStatusButton', function () {
    return {
      restrict: 'AE',
      scope: {
        'currentStatus': '@currentStatus'
      },
      templateUrl: 'views/screenshotStatusButton.html',
      controller: function ($scope) {
        var allStatusses = ['PENDING', 'ACCEPTED', 'REJECTED'];
        $scope.otherStatusses = ['PENDING'];

        function updateOtherStatusses () {
          var currentStatus = $scope.currentStatus || 'PENDING';

            var others = allStatusses.filter(function (status) {
              return status !== currentStatus.toUpperCase();
            });

            $scope.otherStatusses = others;

        };

        $scope.$on('vr-dropdown-open', function (scope) {
          updateOtherStatusses();
        });

        updateOtherStatusses();

        $scope.setStatus = function(newStatus) {
          $scope.$emit('vr-screenshot-status-button-selection', newStatus);
        }

      }
    };
  });

