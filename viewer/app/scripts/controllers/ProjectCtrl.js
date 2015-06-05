'use strict'

angular.module('visualDiffViewerApp')
  .controller('ProjectCtrl', function ($scope, $routeParams, ProjectResource, SuiteResource, ResourceActionWrapper) {
    $scope.projectId = $routeParams.projectId;

    function getProject() {
      $scope.project = ResourceActionWrapper(ProjectResource.getById({projectId: $scope.projectId}));
    }
    getProject();

    $scope.deleteSuite = function (name, suiteId, projectId) {
      if (confirm("Are you sure you want to delete '" + name + "' ?")) {
        $scope.deletedSuite = ResourceActionWrapper(SuiteResource.remove({projectId: projectId, suiteId: suiteId}));
        $scope.deletedSuite.$promise.then(getProject)
      }
    }
  });