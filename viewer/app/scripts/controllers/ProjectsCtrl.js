'use strict';

angular.module('visualDiffViewerApp')
  .controller('ProjectsCtrl', function ($scope, ResourceActionWrapper, ProjectResource) {
    var getProjects = function() {
      $scope.projects = ResourceActionWrapper(ProjectResource.list());
    };


    $scope.createNewProject = function() {
      var newProjectName = $scope.newProjectName;
      if (!newProjectName) {
        return;
      }

      $scope.createdProject = ResourceActionWrapper(ProjectResource.create({name: newProjectName}));
      $scope.createdProject.$promise.then(
          function() {
            $scope.showNewProjectForm = false;
            getProjects();
          }
      ).finally(function() {
          $scope.creatingNewProject = false;
          delete $scope.newProjectName;
      });
    };


    getProjects();
  });