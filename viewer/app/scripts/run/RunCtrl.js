'use strict';

angular.module('visualDiffViewerApp')

  .controller('RunCtrl', function ($scope, $routeParams, filterFilter, RunResource, diffConstants, TitleService, humanejs) {
    TitleService.setTitle('Run ' + $routeParams.runId);

    $scope.model = {}; // always pass empty object

    var toast = humanejs.create({baseCls: 'humane-jackedup', addnCls: 'humane-jackedup-success'});

    var runId = $routeParams.runId;

    switch ($routeParams.filter) {
      case 'rp':
        $scope.currFilter = [diffConstants.pending(), diffConstants.rejected()]
        break;

      default:
        $scope.currFilter = diffConstants.all();
        break;
    }

    $scope.allDiffs = [];

    function updateTotals() {
      var newTotals = {
        pending: 0,
        rejected: 0,
        accepted: 0,
        all: 0
      };

      angular.forEach($scope.allDiffs, function (result) {
        newTotals.all++;
        switch (result.status) {
          case diffConstants.pending() :
            newTotals.pending++;
            break;

          case diffConstants.rejected() :
            newTotals.rejected++;
            break;

          case diffConstants.accepted() :
            newTotals.accepted++;
        }
      });

      $scope.totals = newTotals;
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
      currResult.status = currResult.status == newValue ? diffConstants.pending() : newValue;
      $scope.statusUpdated();
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

    $scope.selectNextDiff = function () {
      $scope.selectedDiffIndex < $scope.diffs.length - 1 && $scope.selectedDiffIndex++;
    };

    $scope.selectPreviousDiff = function () {
      $scope.selectedDiffIndex && $scope.selectedDiffIndex--;
    };

    $scope.selectDiff = function (newIndex) {
      $scope.selectedDiffIndex = newIndex;
    };

    $scope.statusUpdated = function () {
      persistCurrentDiffStatus();
      updateTotals();

      var totals = $scope.totals;
      if (totals.pending === 0 && totals.rejected === 0 && totals.accepted === totals.all) {
        toast.log('All screenshots in this run have been accepted');
      }
    };

    $scope.reapplyFilter = function () {
      applyFilter();
    };

    function applyFilter() {
      $scope.diffs = filterFilter($scope.allDiffs, function (diff) {
        return $scope.currFilter.indexOf(diff.status) != -1;
      });
      updateTotals();

      if ($scope.selectedDiffIndex > $scope.diffs.length - 1) {
        $scope.selectedDiffIndex = Math.max($scope.diffs.length - 1, 0);
      }
    }

    $scope.$watch('currFilter', function () {
      applyFilter();
    });

    var keyPressMap = {
      'LEFT': selectBaselineScreenshot,
      'RIGHT': selectActualScreenshot,
      'UP': $scope.selectPreviousDiff,
      'DOWN': $scope.selectNextDiff,
      'A': function () {
        toggleCurrentStatus(diffConstants.accepted())
      },
      'X': function () {
        toggleCurrentStatus(diffConstants.rejected())
      },
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
        $scope.allDiffs = analysisData.diffs;
        applyFilter();
        updateTotals();
      });
  });
