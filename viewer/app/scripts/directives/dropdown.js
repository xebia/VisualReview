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
  .constant('VR_DROPDOWN_TOGGLE_MESSAGE', 'vr-dropdown-toggle')
  .controller('dropdownCtrl', function ($rootScope, VR_DROPDOWN_TOGGLE_MESSAGE) {
    this.toggleDropdown = function (clickEvent, selfDropdownName, scope) {
        $rootScope.$broadcast(VR_DROPDOWN_TOGGLE_MESSAGE, selfDropdownName);
        if (clickEvent) {
          clickEvent.stopPropagation();
        }

        scope.$digest();
    };
  })
  .directive('dropdownContents', function ($rootScope, VR_DROPDOWN_TOGGLE_MESSAGE, $window) {
    return {
      restrict: 'AE',
      controller: 'dropdownCtrl',
      compile: function(element){
        var measureWrapperClass = 'vr-dropdown-menu-measure-wrapper';
        element[0].innerHTML = '<div class="' + measureWrapperClass + '">' + element[0].innerHTML + '</div>';

        function getHeight(contentsElement) {
          return contentsElement[0].querySelector('.' + measureWrapperClass).clientHeight + "px";
        }

        return {
          post: function(scope, element, attrs, ctrl){
            element.addClass("vr-dropdown-menu-contents");
            element[0].style.height = getHeight(element);

            var selfDropdownName = attrs.dropdownContents,
              isOpened = false,
              toggleDropdown = function(e) {
                ctrl.toggleDropdown(e, selfDropdownName, scope);
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
                element[0].style.height = getHeight(element);
                element[0].style.maxHeight = $window.innerHeight - 20 + "px";
                element[0].style.visibility = "visible";
              } else {
                element[0].style.height = "0px";
                element[0].style.visibility = "hidden";
              }

              isOpened = !isOpened;
            });
          }
        };
      }

    }
  })
  .directive('dropdownToggle', function ($parse) {
    return {
      controller: 'dropdownCtrl',
      link: function(scope, element, attrs, ctrl) {
        var selfDropdownName = attrs.dropdownToggle;

        var toggleDropdown = function (e) {
          var toggleCondition = attrs.dropdownToggleIf;
          if (!toggleCondition || $parse(toggleCondition)(scope) ) {
             ctrl.toggleDropdown(e, selfDropdownName, scope);
          }

          if (e) {
            e.stopPropagation();
          }
        };

        element.bind('click', toggleDropdown);

        scope.$on('$destroy', function() {
          element.unbind('click', toggleDropdown);
        });
      }
    }
  });

