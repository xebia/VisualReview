'use strict'

angular.module('visualDiffViewerApp')
  .controller('ProjectCtrl', function ($scope, $routeParams, ProjectResource, ResourceActionWrapper) {
    $scope.projectId = $routeParams.projectId;


    $scope.project = ResourceActionWrapper(ProjectResource.getById({projectId: $scope.projectId}));


  });