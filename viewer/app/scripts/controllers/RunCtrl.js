'use strict';

angular.module('visualDiffViewerApp')

  .controller('RunCtrl', function ($scope, $routeParams, RunResource, diffConstants) {
		var runId = $routeParams.runId;

    function updateTotals() {
      $scope.totalUndecided = 0;
      $scope.totalRejected = 0;
      $scope.totalAccepted = 0;

      angular.forEach($scope.diffs, function (result) {
        switch (result.status) {
          case diffConstants.pending() :
            $scope.totalUndecided++;
            break;

          case diffConstants.rejected() :
            $scope.totalRejected++;
            break;

          case diffConstants.accepted() :
            $scope.totalAccepted++;
        }
      });
    }

    function persistCurrentDiffStatus() {
      var diffId = $scope.diffs[$scope.selectedDiffIndex].id;
      var runId = $scope.analysis.runId;
      var status = $scope.diffs[$scope.selectedDiffIndex].status.toLowerCase();

			$scope.diffs[$scope.selectedDiffIndex].isPersisted = false;
      RunResource.updateStatus({runId: runId, diffId: diffId}, {status: status});
    }

    function toggleCurrentStatus(newValue) {
      var currResult = $scope.diffs[$scope.selectedDiffIndex];
      currResult.status = currResult.status == newValue ? diffConstants.pending() : newValue;;
			persistCurrentDiffStatus();
    }

    function selectBaselineScreenshot() {
      if ($scope.diffs[$scope.selectedDiffIndex].before) {
        $scope.selectedScreenshot = "before";
      }
    }

    function selectActualScreenshot() {
      $scope.selectedScreenshot = "after";
    }

    function toggleDiff() {
      $scope.showDiff = !$scope.showDiff;
    }

		$scope.selectedDiffIndex = 0;
		$scope.selectedScreenshot = "after";
		$scope.showDiff = true;

		$scope.totalUndecided = 0;
		$scope.totalRejected = 0;
		$scope.totalAccepted = 0;

    $scope.selectNextDiff = function () {
      $scope.selectedDiffIndex < $scope.diffs.length - 1 && $scope.selectedDiffIndex++;
    };

    $scope.selectPreviousDiff = function () {
      $scope.selectedDiffIndex && $scope.selectedDiffIndex--;
    };

    $scope.selectDiff = function (newIndex) {
      $scope.selectedDiffIndex = newIndex;
    };

    $scope.statusUpdated = function() {
      persistCurrentDiffStatus();
    };


    var keyPressMap = {
      'LEFT': selectBaselineScreenshot,
      'RIGHT': selectActualScreenshot,
      'UP': $scope.selectPreviousDiff,
      'DOWN': $scope.selectNextDiff,
      'A': function () {toggleCurrentStatus( diffConstants.accepted())},
      'X': function () {toggleCurrentStatus( diffConstants.rejected())},
      'D': toggleDiff
    };

    $scope.$on('keydown', function (ngEvent, key) {
      $scope.$apply(function () {
        $scope.$emit("selectionChange", $scope.selectedScreenshot, $scope.selectedDiffIndex);
        keyPressMap[key]();
        updateTotals();
      });
    });

    $scope.$on('vr-screenshot-select', function (scope, newIndex) {
      $scope.selectedDiffIndex = newIndex;
    });

    $scope.$watch('run', function () {
      $scope.$emit("selectionChange", $scope.selectedScreenshot);
    });

    RunResource.get({runId: runId}).$promise
      .then(function (runData) {
        $scope.run = runData;
        return RunResource.getAnalysis({runId: runId}).$promise;
      })
      .then(function (analysisData) {
        $scope.analysis = analysisData.analysis;
        $scope.diffs = analysisData.diffs;
        updateTotals();
      });

  });
