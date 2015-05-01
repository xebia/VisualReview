/*
 * Copyright 2015 Xebia B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

'use strict';

angular.module('visualDiffViewerApp')
  .constant('VR_DROPDOWN_TOGGLE_MESSAGE', "vr-dropdown-toggle")
  .directive('vrDropdownContents', function ($rootScope, VR_DROPDOWN_TOGGLE_MESSAGE) {
    return {
      restrict: 'AE',
      link: function (scope, element, attrs) {
        element.addClass("vr-dropdown-menu-contents");
        var selfDropdownName = attrs.vrDropdownContents,
            isOpened = false,
            initialHeight = 0,
            toggleDropdown = function(e) {
              $rootScope.$broadcast(VR_DROPDOWN_TOGGLE_MESSAGE, selfDropdownName);
            };

        element.bind('click', toggleDropdown);

        scope.$on('$destroy', function() {
          element.unbind('click', toggleDropdown);
        });

        $rootScope.$on(VR_DROPDOWN_TOGGLE_MESSAGE, function(event, dropdownName) {
          if (dropdownName !== selfDropdownName) {
            return;
          }

          if (!isOpened) {
            if (!initialHeight) {
              initialHeight = element[0].offsetHeight;
            }

            element[0].style.height = initialHeight + "px";
            element[0].style.visibility = "visible";
          } else {
            element[0].style.height = "0px";
            element[0].style.visibility = "hidden";
          }

          isOpened = !isOpened;

        });
      }
    }
  })
  .directive('vrDropdownToggle', function ($rootScope, VR_DROPDOWN_TOGGLE_MESSAGE) {
    return {
      link: function(scope, element, attrs) {
        var selfDropdownName = attrs.vrDropdownToggle;

        var toggleDropdown = function (e) {
          $rootScope.$broadcast(VR_DROPDOWN_TOGGLE_MESSAGE, selfDropdownName);
          if (e) {
            e.stopPropagation();
          }
          scope.$digest();
        };

        element.bind('click', toggleDropdown);

        scope.$on('$destroy', function() {
          element.unbind('click', toggleDropdown);
        });
      }
    }
  });

