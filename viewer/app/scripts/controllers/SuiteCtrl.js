'use strict'

angular.module('visualDiffViewerApp')
  .controller('SuiteCtrl', function ($scope, $routeParams, ResourceActionWrapper, SuiteResource, TitleService) {
    TitleService.setTitle('Suite ' + $routeParams.suiteId);

    $scope.projectId = $routeParams.projectId;
    $scope.suiteId = $routeParams.suiteId;

    $scope.suite = ResourceActionWrapper(SuiteResource.get({projectId: $scope.projectId, suiteId: $scope.suiteId}));

  });