'use strict';

angular.module('visualDiffViewerApp')
  .factory('ResourceActionWrapper', function () {
    return function (resourceAction) {
      resourceAction.$vrIsLoading = true;
      resourceAction.$vrHasError = false;
      resourceAction.$vrErrorMessage = '';

      resourceAction.$promise.catch(function (err) {
        resourceAction.$vrHasError = true;
        resourceAction.$vrErrorMessage = (err.data || '');
      });
      resourceAction.$promise.finally(function () {
        resourceAction.$vrIsLoading = false;
      });

      return resourceAction;
    };
  });
