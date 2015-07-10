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
  .factory('CameraService', function (MathService) {
    var zoomStep = 1.05,
      minZoom = 0.5,
      maxZoom = 4.0;

    /**
     * Constraint panning so image is always visible for a quarter or more.
     * @param offset
     */
    function constraintPan(camera) {
      var height2 = $(window).height() / 2.0,
        imgHeight = $('.zoom-frame').height(),
        imgWidth = $('.zoom-frame').width(),
        xBorder = imgWidth / 2.0 * camera.scale;

      camera.x = MathService.clamp(camera.x, -xBorder, xBorder);
      camera.y = MathService.clamp(camera.y, height2 - imgHeight * camera.scale, height2);
    }

    function reset(camera) {
      camera.scale = 1;
      camera.x = 0;
      camera.y = 0;
    }

    function pan(camera, delta) {
      camera.x += delta.x;
      camera.y += delta.y;

      constraintPan(camera);
    }

    function zoom(camera, zoomPoint, delta) {
      var factor = delta > 0.0 ? zoomStep: 1.0 / zoomStep,
        newScale = camera.scale * factor;
      if (newScale < minZoom || newScale > maxZoom) {
        return;
      }
      camera.scale = newScale;

      // Zoom with respect to center point in pan frame.
      // Move images, scale and move them back.
      if (camera.scale > 1.0) {
        camera.x = (camera.x - zoomPoint.x) * factor + zoomPoint.x;
        camera.y = (camera.y - zoomPoint.y) * factor + zoomPoint.y;
      }

    }

    return {
      pan: pan,
      reset: reset,
      zoom: zoom
    };
  });