'use strict';

angular.module('visualDiffViewerApp')
  .constant('DIFF_STATUS_ACCEPTED', 'accepted')
  .constant('DIFF_STATUS_REJECTED', 'rejected')
  .constant('DIFF_STATUS_PENDING', 'pending')
  .factory('diffConstants', function (DIFF_STATUS_ACCEPTED, DIFF_STATUS_REJECTED, DIFF_STATUS_PENDING) {
      return {
        accepted: _.constant(DIFF_STATUS_ACCEPTED),
        rejected: _.constant(DIFF_STATUS_REJECTED),
        pending: _.constant(DIFF_STATUS_PENDING),
        all: _.constant([DIFF_STATUS_ACCEPTED, DIFF_STATUS_REJECTED, DIFF_STATUS_PENDING])
      }
  });