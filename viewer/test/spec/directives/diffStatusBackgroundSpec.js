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

describe('Directive: diff-status-background', function () {

	beforeEach(module('visualDiffViewerApp'));

	var	scope,
		$compile,
		diffConstants,
		element;

	beforeEach(inject(function ($rootScope, _$compile_, _diffConstants_) {
		$compile = _$compile_;
		diffConstants = _diffConstants_;

		scope = $rootScope.$new();

		scope.myStatus = diffConstants.rejected();

		element = compileAngularHtml('<div diff-status-background="myStatus"></div>', scope, $compile);
	}));

	it('should add a background class on the element corresponding to the given status', function() {
		expect(element.attr('class')).toBe('ng-scope background-light-red');

		scope.myStatus = diffConstants.accepted();
		scope.$digest();
		expect(element.attr('class')).toBe('ng-scope background-light-green');

		scope.myStatus = diffConstants.pending();
		scope.$digest();
		expect(element.attr('class')).toBe('ng-scope background-light-gray');
	});


});
