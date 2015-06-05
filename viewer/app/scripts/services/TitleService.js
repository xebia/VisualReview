'use strict';

angular.module('visualDiffViewerApp')
  .factory('TitleService', function () {
    return {
      setTitle: function (title) {
        document.title = title + ' | VisualReview';
      }
    };
  });
