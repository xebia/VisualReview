'use strict';

angular.module('visualDiffViewerApp')
  .constant("SCREENSHOT_METADATA_UNKNOWN", 'unknown')
  .filter('screenshotMetadataOs', function(SCREENSHOT_METADATA_UNKNOWN) {
    return function(input) {
      if (!input) {
        return SCREENSHOT_METADATA_UNKNOWN;
      }

      switch(input.toLowerCase()) {
        case 'windows':
        case 'xp':
        case 'vista':
          return 'windows-old';

        case 'win8':
        case 'win8_1':
          return 'windows';

        case 'mac':
        case 'snow_leopard':
        case 'mountain_lion':
        case 'mavericks':
        case 'yosemite':
          return 'mac';

        case 'linux':
          return 'linux';

        case 'android':
          return 'android';

        default:
          return SCREENSHOT_METADATA_OS_UNKNOWN;
      }
    }
  })
  .filter('screenshotMetadataBrowser', function(SCREENSHOT_METADATA_UNKNOWN) {
    return function(input) {
      if (!input) {
        return SCREENSHOT_METADATA_UNKNOWN;
      }

      var lowercaseInput = input.toLowerCase();
      switch(lowercaseInput) {
        case 'android':
        case 'safari':
        case 'chrome':
        case 'firefox':
        case 'opera':
          return lowercaseInput;

        case 'internet explorer':
          return 'ie';

        default:
          return SCREENSHOT_METADATA_UNKNOWN;
      }
    }
  })
  .directive('screenshotMetadata', function(SCREENSHOT_METADATA_UNKNOWN) {
    return {
      restrict: 'A',
      scope: {
        os: '=',
        browser: '=',
        version: '=',
        resolution: '='
      },
      templateUrl: 'views/screenshotMetadata.html',
      link: function(scope) {
        scope.unknown = SCREENSHOT_METADATA_UNKNOWN;
      }
    };
  });
