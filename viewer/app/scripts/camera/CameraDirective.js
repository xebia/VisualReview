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

angular.module('visualDiffViewerApp')
  .directive('camera', function (CameraService, hamsterjs) {
    return {
      restrict: 'E',
      scope: {},
      transclude: true,
      templateUrl: 'scripts/camera/cameraDirective.html',
      link: function (scope, elem) {
        var frameElement = elem,
          panElement = elem.find('.pan-frame'),
          zoomElement = elem.find('.zoom-frame'),
          isDragging = false,
          prevMousePt;

        function startDrag(vPoint) {
          prevMousePt = {
            x: vPoint.x,
            y: vPoint.y
          };
          isDragging = true;
        }

        function stopDrag() {
          isDragging = false;
        }

        /**
         * Calculate mouse position with respect to top left of camera frame
         * @param vPoint
         * @returns {{x: number, y: number}}
         */
        function viewToFrame(vPoint) {
          return {
            x: vPoint.x - frameElement.offset().left,
            y: vPoint.y - frameElement.offset().top
          };
        }

        function onMouseDown(event) {
          startDrag({x: event.pageX, y: event.pageY});
        }

        function onMouseMove(event) {
          if (isDragging) {
            var delta = {
              x: event.pageX - prevMousePt.x,
              y: event.pageY - prevMousePt.y
            };

            CameraService.pan(panElement, zoomElement, delta);

            prevMousePt = {
              x: event.pageX,
              y: event.pageY
            };
          }
        }

        function onMouseUp() {
          stopDrag();
        }

        function onMouseLeave() {
          stopDrag();
        }

        scope.onMouseWheel = function (event, delta, deltaX, deltaY) {
          var zoomDelta = deltaY,
            mousePoint = viewToFrame({x: event.originalEvent.pageX, y: event.originalEvent.pageY});

          CameraService.zoom(panElement, zoomElement, mousePoint, zoomDelta);

          event.preventDefault();
        };

        elem.bind('dragstart', function () {
          return false; //disable default drag behavior
        });

        hamsterjs(elem[0]).wheel(scope.onMouseWheel);
        elem.bind('mousedown', onMouseDown);
        elem.bind('mouseup', onMouseUp);
        elem.bind('mousemove', onMouseMove);
        elem.bind('mouseleave', onMouseLeave);
      }
    };
  });