'use strict';

angular.module('visualDiffViewerApp')
  .directive('vrDropdown', function () {
    return {
      restrict: 'AE',
      controller: function($scope) {
        var isOpen = false;

        this.toggle = function() {
          isOpen = !isOpen;

          var scopeMessage = isOpen ? "vr-dropdown-open" : "vr-dropdown-close";
          $scope.$broadcast(scopeMessage, this.dropdownName);
        }
      },
      link: function(scope, element, attrs, vrDropdownCtrl) {
        vrDropdownCtrl.dropdownName = attrs.vrDropdown;
      }
    };
  })
  .directive('vrDropdownContents', function ($timeout) {
    return {
      restrict: 'AE',
      require: '^vrDropdown',
      link: function (scope, element, attrs, vrDropdownCtrl) {
        element.addClass("vr-dropdown-menu-contents");
        var initialHeight = 0;

        var closeDropdown = function(e) {
          vrDropdownCtrl.toggle();
        }

        element.bind('click', closeDropdown);

        scope.$on('$destroy', function() {
          element.unbind('click', closeDropdown);
        });

        scope.$on('vr-dropdown-open', function(event, dropdownName) {
          if (dropdownName !== vrDropdownCtrl.dropdownName) {
            return;
          }

          if (!initialHeight) {
            initialHeight = element[0].offsetHeight;
          }

          element[0].style.height = initialHeight + "px";
          element[0].style.visibility = "visible";
        });

        scope.$on('vr-dropdown-close', function(event, dropdownName) {
          if (dropdownName !== vrDropdownCtrl.dropdownName) {
            return;
          }

          element[0].style.height = "0px";
          element[0].style.visibility = "hidden";
        });
      }
    }
  })
  .directive('vrDropdownToggle', function () {
    return {
      require: '^vrDropdown',
      link: function(scope, element, attrs, vrDropdownCtrl) {
        var toggleDropdown = function () {
          vrDropdownCtrl.toggle();
          scope.$digest();
        };

        element.bind('click', toggleDropdown);

        scope.$on('$destroy', function() {
          element.unbind('click', toggleDropdown);
        });
      }
    }
  });

