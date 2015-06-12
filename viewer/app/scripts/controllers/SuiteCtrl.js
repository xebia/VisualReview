'use strict'

angular.module('visualDiffViewerApp')
  .controller('SuiteCtrl', function ($scope, $routeParams, ResourceActionWrapper, SuiteResource, RunResource) {
    $scope.projectId = $routeParams.projectId;
    $scope.suiteId = $routeParams.suiteId;

    var getSuite = function() {
      $scope.suite = ResourceActionWrapper(SuiteResource.get({projectId: $scope.projectId, suiteId: $scope.suiteId}));
    };
    getSuite();

    $scope.deleteRun = function (id) {
      if (confirm("Are you sure you want to delete run '" + id + "' ?")) {
        $scope.deletedRun = ResourceActionWrapper(RunResource.remove({runId: id}));
        $scope.deletedRun.$promise.then(getSuite)
      }
    };

  });