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

describe('Directive: conditional link', function () {

  beforeEach(module('visualDiffViewerApp'));

  var scope,
    $compile;

  function compileHtml(html, scope) {
    var element = angular.element(html);
    element = $compile(element)(scope);
    scope.$digest();
    return element;
  }

  beforeEach(inject(function ($rootScope, _$compile_) {
    scope = $rootScope.$new();
    scope.text = 'foo bar';
    $compile = _$compile_;
  }));

  it('should show a text span if show-link is false', function () {
    var element = compileHtml('<conditional-link text="text"></conditional-link>', scope, $compile);
    expect(element.text()).toBe(scope.text);
    expect(element.find('a').length).toBe(0);
  });

  it('should show a link with the href url if show-link is true', function () {
    scope.link = '/test';
    scope.showLink = true;
    var element = compileHtml(
      '<conditional-link href="{{link}}" show-link="showLink" text="text"></conditional-link>'
      , scope, $compile);
    expect(element.text()).toBe(scope.text);
    expect(element.find('a').attr('href')).toBe(scope.link);
  });

});
