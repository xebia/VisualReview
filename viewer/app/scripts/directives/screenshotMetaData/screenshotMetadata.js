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
        case 'mac os x':
          return 'mac';

        case 'linux':
          return 'linux';

        case 'android':
          return 'android';

        default:
          return SCREENSHOT_METADATA_UNKNOWN;
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
        meta: '=',
        properties: '='
      },
      templateUrl: 'scripts/directives/screenshotMetaData/screenshotMetadata.html',
      link: function(scope) {

        function takeFromPropertiesOrMeta(field, meta, properties) {
          scope[field] = (properties && properties[field]) || (meta && meta[field]);
        }

        scope.$watchGroup(['meta', 'properties'], function(newValues) {
          ['os', 'browser', 'version', 'resolution'].forEach(function (field) {
            takeFromPropertiesOrMeta(field, newValues[0], newValues[1]);
          });
        });

        scope.unknown = SCREENSHOT_METADATA_UNKNOWN;
      }
    };
  });
