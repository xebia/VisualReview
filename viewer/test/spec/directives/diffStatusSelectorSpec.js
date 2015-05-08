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

describe('Directive: diff-status-selector', function () {

	beforeEach(module('visualDiffViewerApp'));

	var	scope,
		$compile,
		diffConstants,
		element;

	function testIconStatusOnElement(expectedStatus, element) {
		var allDiffStatuses = diffConstants.all();
		for(var i = 0; i < allDiffStatuses.length; i++) {
			var diffStatus = allDiffStatuses[i];
			var expectedNrOfElements = 0;
			if (diffStatus === expectedStatus) {
				expectedNrOfElements = 1;
			}

			expect(element.find('.icon-' + diffStatus).length)
				.toBe(expectedNrOfElements, 'icon-' + diffStatus +' should be rendered ' + expectedNrOfElements + ' time(s)');
		}
	}

	beforeEach(inject(function ($rootScope, _$compile_, _diffConstants_) {
		$compile = _$compile_;
		diffConstants = _diffConstants_;

		scope = $rootScope.$new();

		scope.myDiff = {
			status: diffConstants.rejected()
		};

		element = compileAngularHtml('<div diff-status-selector="myDiff"></div>', scope, $compile);
	}));

	it('should display the current diff\'s status and following updates of it\'s value', function() {
		var dropdownButton = element.find('[dropdown-toggle]');
		testIconStatusOnElement(diffConstants.rejected(), dropdownButton);

		scope.myDiff.status = diffConstants.accepted();
		scope.$digest();
		testIconStatusOnElement(diffConstants.accepted(), dropdownButton);
	});

	it('should change the given diff\'s status when clicking on an item in the dropdown', function() {
		var dropdownContents = element.find('[dropdown-contents]');

		expect(scope.myDiff.status).toBe(diffConstants.rejected());

		dropdownContents.find('.icon-accepted').click();
		expect(scope.myDiff.status).toBe(diffConstants.accepted());

		dropdownContents.find('.icon-rejected').click();
		expect(scope.myDiff.status).toBe(diffConstants.rejected());

		dropdownContents.find('.icon-pending').click();
		expect(scope.myDiff.status).toBe(diffConstants.pending());

		dropdownContents.find('.icon-pending').click(); // test if the dropdown is not toggling
		expect(scope.myDiff.status).toBe(diffConstants.pending());
	});

	it('should call the on-status-update expression when given', function () {
		scope.myStatusUpdateListener = function () {};
		spyOn(scope, 'myStatusUpdateListener');

		element = compileAngularHtml('<div diff-status-selector="myDiff" on-status-selected="myStatusUpdateListener()"></div>', scope, $compile);

		var dropdownContents = element.find('[dropdown-contents]');
		dropdownContents.find('.icon-accepted').click();
		dropdownContents.find('.icon-rejected').click();
		dropdownContents.find('.icon-accepted').click();
		dropdownContents.find('.icon-pending').click();

		expect(scope.myStatusUpdateListener.calls.count()).toEqual(4);
	});
});
