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
  .factory('GeometricTransformation', function (mathjs) {

    /**
     * Create homogenous translation matrix
     * @param offset, object with x and y property
     * @returns mathjs matrix
     */
    function translation(offset) {
      return mathjs.matrix(
        [ [1.0, 0.0, offset.x],
          [0.0, 1.0, offset.y],
          [0.0, 0.0, 1.0]]
      );
    }

    /**
     * Create homogeneous scale matrix
     * @param s, uniform scale factor
     * @returns mathjs matrix
     */
    function scale(s) {
      return mathjs.matrix(
        [ [s,   0.0, 0.0],
          [0.0, s,   0.0],
          [0.0, 0.0, 1.0]]
      );
    }

    /**
     * Create homogeneous scale about point matrix
     * @param factor, uniform scale factor
     * @param point, object with x and y property
     * @returns mathjs matrix
     */
    function scaleAbout(factor, point) {
      var T = translation(point),
        S = scale(factor),
        Tinv = mathjs.inv(T);
      return T.multiply(S.multiply(Tinv));
    }

    return {
      translation: translation,
      scale: scale,
      scaleAbout: scaleAbout
    };
  });