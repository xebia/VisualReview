'use strict';

angular.module('visualDiffViewerApp').constant('DIFF_STATUS_ACCEPTED', 'accepted')
  .constant('DIFF_STATUS_REJECTED', 'rejected')
  .constant('DIFF_STATUS_PENDING', 'pending')
  .factory('diffConstants', function (DIFF_STATUS_ACCEPTED, DIFF_STATUS_REJECTED, DIFF_STATUS_PENDING) {
      function createGetter(value) {
        return function() {
          return value;
        }
      }
      return {
        accepted: createGetter(DIFF_STATUS_ACCEPTED),
        rejected: createGetter(DIFF_STATUS_REJECTED),
        pending: createGetter(DIFF_STATUS_PENDING),
        all: createGetter([DIFF_STATUS_ACCEPTED, DIFF_STATUS_REJECTED, DIFF_STATUS_PENDING])
      }
  });