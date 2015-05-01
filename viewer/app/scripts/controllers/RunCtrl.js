'use strict';

angular.module('visualDiffViewerApp')
  .controller('RunCtrl', function ($scope, $routeParams, RunResource) {
    $scope.runId = $routeParams.runId;

    function updateTotals() {
      $scope.totalUndecided = 0;
      $scope.totalRejected = 0;
      $scope.totalAccepted = 0;

      angular.forEach($scope.diffs, function (result) {
        switch (result.status) {
          case "pending" :
            $scope.totalUndecided++;
            break;

          case "rejected" :
            $scope.totalRejected++;
            break;

          case "accepted" :
            $scope.totalAccepted++;
        }
      });
    }

    function saveState() {
      var diffId = $scope.diffs[$scope.selectedDiffIndex].id;
      var runId = $scope.analysis.runId;
      var status = $scope.diffs[$scope.selectedDiffIndex].status.toLowerCase();

      RunResource.updateStatus({runId: runId, diffId: diffId}, {status: status});
    }

    function toggleAccepted() {
      var currResult = $scope.diffs[$scope.selectedDiffIndex];
      currResult.status == "accepted" ? currResult.status = "pending" : currResult.status = "accepted";
      saveState();
    }

    function toggleRejected() {
      var currResult = $scope.diffs[$scope.selectedDiffIndex];
      currResult.status == "rejected" ? currResult.status = "pending" : currResult.status = "rejected";
      saveState();
    }

    function selectBaselineScreenshot() {
      $scope.selectedScreenshot = "before";
    }

    function selectActualScreenshot() {
      $scope.selectedScreenshot = "after";
    }

    function toggleDiff() {
      $scope.showDiff = !$scope.showDiff;
    }

    $scope.selectNextScreenshot = function () {
      $scope.selectedDiffIndex < $scope.diffs.length - 1 && $scope.selectedDiffIndex++;
    };

    $scope.selectPreviousScreenshot = function () {
      $scope.selectedDiffIndex && $scope.selectedDiffIndex--;
    };

    $scope.selectedDiffIndex = 0;
    $scope.selectedScreenshot = "after";
    $scope.showDiff = true;

    $scope.totalUndecided = 0;
    $scope.totalRejected = 0;
    $scope.totalAccepted = 0;


    var keyPressMap = {
      'LEFT': selectBaselineScreenshot,
      'RIGHT': selectActualScreenshot,
      'UP': $scope.selectPreviousScreenshot,
      'DOWN': $scope.selectNextScreenshot,
      'A': toggleAccepted,
      'X': toggleRejected,
      'D': toggleDiff
    };

    $scope.$on('keydown', function (ngEvent, key) {
      $scope.$apply(function () {
        $scope.$emit("selectionChange", $scope.selectedScreenshot, $scope.selectedDiffIndex);
        keyPressMap[key]();
        updateTotals();
      });
    });

    $scope.$on('vr-screenshot-status-button-selection', function (scope, newValue) {
      var currResult = $scope.diffs[$scope.selectedDiffIndex];
      currResult.status = newValue;
      saveState();
    });

    $scope.$on('vr-screenshot-select', function (scope, newIndex) {
      $scope.selectedDiffIndex = newIndex;
    });

    $scope.$watch('run', function () {
      $scope.$emit("selectionChange", $scope.selectedScreenshot);
    });

    RunResource.get({runId: $scope.runId}).$promise
      .then(function (runData) {
        $scope.run = runData;
        return RunResource.getAnalysis({runId: $scope.runId}).$promise;
      })
      .then(function (analysisData) {
        $scope.analysis = analysisData.analysis;
        $scope.diffs = analysisData.diffs;
        updateTotals();
      });

  });
