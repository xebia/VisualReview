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
  .directive('diffStatusBackground', function () {
    return {
      restrict: 'AE',
      link: function (scope, element, attrs) {

        var getStyleByDiffStatus = function (diffStatus) {
          switch(diffStatus) {
            case 'pending':
              return 'background-light-gray';
            case 'rejected':
              return 'background-light-red';
            case 'accepted':
              return 'background-light-green';
          }
        };

        scope.$watch(attrs.diffStatusBackground, function(newValue, oldValue) {
          element.removeClass(getStyleByDiffStatus(oldValue));
          element.addClass(getStyleByDiffStatus(newValue));
        });
      }
    }
  });