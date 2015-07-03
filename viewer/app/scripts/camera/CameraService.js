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
  .factory('CameraService', function (GeometricTransformation, MathService) {
    var offset = GeometricTransformation.translation({ x: 0.0, y: 0.0 }), //translation matrix
      scale  = GeometricTransformation.scale(1.0), //uniform scaling matrix
      zoomStep = 1.5,
      minZoom = 0.5,
      maxZoom = 4.0;

    const xIndex = [0, 2],
      yIndex = [1, 2];

    function getZoom() {
      return scale.get([0, 0]);
    }

    function applyPanZoom(camera) {
      camera.zoom = getZoom();
      camera.left = offset.get(xIndex);
      camera.top = offset.get(yIndex);
    }

    /**
     * Constraint panning so image is always visible for a quarter or more.
     * @param offset
     */
    function constraintPan(offset) {
      var imgHeight = $('.run-view-item').height(),
        imgWidth = $('.run-view-item').width();

      var zoom = getZoom();

      var height2 = $(window).height() / 2.0,
        width2 = $(window).width() / 2.0;
      offset.set(xIndex, MathService.clamp(offset.get(xIndex), width2 - imgWidth * zoom, width2));
      offset.set(yIndex, MathService.clamp(offset.get(yIndex), height2 - imgHeight * zoom, height2));
    }

    function centerHorizontal(camera) {
      var imgWidth = $('.run-view-item').width();
      var zoom = getZoom();
      var width2 = $(window).width() / 2.0;
      camera.left = width2 - imgWidth / 2.0 * zoom;
    }

    function reset(camera) {
      centerHorizontal(camera);
      camera.top = 0;
      camera.zoom = 0;
      offset.set(xIndex, camera.left);
      offset.set(yIndex, camera.top);
      scale = GeometricTransformation.scale(1.0);
    }

    function pan(camera, delta) {
      var zoom = getZoom();
      delta = {
        x: delta.x / zoom,
        y: delta.y / zoom
      };
      offset = offset.multiply(GeometricTransformation.translation(delta));

      constraintPan(offset);

      applyPanZoom(camera);
    }

    function zoom(camera, centerVPoint, delta) {
      var factor;
      if (delta > 0.0) {
        factor = zoomStep;
      } else {
        factor = 1.0 / zoomStep;
      }
      var currentZoom = getZoom(),
        newZoom = currentZoom * factor;
      if (newZoom < minZoom || newZoom > maxZoom) {
        return;
      }

      // Compute transformation that scales about point
      // Zoom = T * S * Tinv
      var Zoom = GeometricTransformation.scaleAbout(factor, centerVPoint);
      scale = scale.multiply(Zoom);

      // Zoom with respect to center point in pan frame
      var Ap = mathjs.inv(offset).multiply(GeometricTransformation.translation(centerVPoint));
      var Apv = { x: Ap.get(xIndex), y: Ap.get(yIndex)};
      Zoom = GeometricTransformation.scaleAbout(factor, Apv);
      offset = offset.multiply(Zoom);

      applyPanZoom(camera);
    }

    return {
      pan: pan,
      reset: reset,
      zoom: zoom
    };
  });