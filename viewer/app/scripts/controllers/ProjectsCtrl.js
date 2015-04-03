'use strict';

angular.module('visualDiffViewerApp')
  .controller('ProjectsCtrl', function ($scope, ResourceActionWrapper, ProjectResource) {
    var getProjects = function() {
      $scope.projects = ResourceActionWrapper(ProjectResource.list());
    };


    $scope.createNewProject = function () {
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

    $scope.deleteProject = function (projectName, projectId) {
      if (confirm("Are you sure you want to delete '" + projectName + "' ?")) {
        $scope.deletedProject = ResourceActionWrapper(ProjectResource.remove({projectId: projectId}));
        $scope.deletedProject.$promise.then(getProjects)
      }
    };

    getProjects();
  });