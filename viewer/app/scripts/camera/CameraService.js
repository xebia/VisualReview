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
  .factory('CameraService', function (GeometricTransformation) {
    var offset = GeometricTransformation.translation({ x: 0.0, y: 0.0 }), //translation matrix
      scale  = GeometricTransformation.scale(1.0), //uniform scaling matrix
      zoomStep = 1.5,
      minZoom = 0.5,
      maxZoom = 4.0;

    function getZoom() {
      return scale.get([0, 0]);
    }

    function applyPanZoom(panElement, zoomElement) {
      zoomElement.css('zoom', getZoom());
      panElement.css('left', offset.get([0, 2]));
      panElement.css('top',  offset.get([1, 2]));
    }

    function setPosition(panElement, zoomElement, viewPoint) {
      offset = GeometricTransformation.translation(viewPoint);
      applyPanZoom(panElement, zoomElement);
    }

    function pan(panElement, zoomElement, delta) {
      var zoom = getZoom();
      delta = {
        x: delta.x / zoom,
        y: delta.y / zoom
      };
      offset = offset.multiply(GeometricTransformation.translation(delta));

      applyPanZoom(panElement, zoomElement);
    }

    function zoom(panElement, zoomElement, centerVPoint, delta) {
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
      var Apv = { x: Ap.get([0, 2]), y: Ap.get([1, 2])}
      Zoom = GeometricTransformation.scaleAbout(factor, Apv);
      offset = offset.multiply(Zoom);

      applyPanZoom(panElement, zoomElement);
    }

    return {
      pan: pan,
      zoom: zoom
    };
  });