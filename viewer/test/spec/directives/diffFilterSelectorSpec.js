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

describe('Directive: diff-filter-selector', function () {

	var	scope,
		$compile,
		diffConstants;

	function createTotalsObject(nrOfAccepted, nrOfRejected, nrOfPending) {
		return {
			accepted: nrOfAccepted,
			rejected: nrOfRejected,
			pending: nrOfPending,
			all: nrOfAccepted + nrOfRejected + nrOfPending
		}
	}

	function createElement() {
		return compileAngularHtml('<div diff-filter-selector="myFilter" totals="myTotals"></div>', scope, $compile);
	}

	beforeEach(module('visualDiffViewerApp'));

	beforeEach(inject(function ($rootScope, _$compile_, _diffConstants_) {
		$compile = _$compile_;
		diffConstants = _diffConstants_;

		scope = $rootScope.$new();
	}));

	describe('the selector link', function () {
		it('should display the currently selected filter given as an attribute', function() {
			scope.myFilter = [diffConstants.accepted(), diffConstants.rejected(), diffConstants.pending()];
			scope.myTotals = createTotalsObject(10, 5, 2);
			var element = createElement();

			var dropdownButton = element.find('[data-test="filter-selector-button"]');
			expect(dropdownButton.text().trim()).toBe('show: all (17)');

			scope.myFilter = [diffConstants.rejected()];
			scope.$digest();
			expect(dropdownButton.text().trim()).toBe('show: only rejected (5)');

			scope.myFilter = [diffConstants.pending(), diffConstants.rejected()];
			scope.$digest();
			expect(dropdownButton.text().trim()).toBe('show: only pending and rejected (7)');
		});

		it('should display the "all" filter when the attribute\'s value is empty', function() {
			scope.myFilter = undefined;
			scope.myTotals = createTotalsObject(10, 5, 2);
			var element = createElement();

			var dropdownButton = element.find('[data-test="filter-selector-button"]');
			expect(dropdownButton.text().trim()).toBe('show: all (17)');
		});

		it('should display the correct totals when they\'re updated on the scope', function() {
			scope.myFilter = [diffConstants.rejected()];
			scope.myTotals = createTotalsObject(10, 5, 2);
			var element = createElement();

			var dropdownButton = element.find('[data-test="filter-selector-button"]');
			expect(dropdownButton.text().trim()).toBe('show: only rejected (5)');

			scope.myTotals = createTotalsObject(10, 4, 2);
			scope.$digest();
			expect(dropdownButton.text().trim()).toBe('show: only rejected (4)');
		});

		it('should open the dropdown menu when clicked on', function() {
			scope.myFilter = [diffConstants.rejected()];
			scope.myTotals = createTotalsObject(10, 5, 2);
			var element = createElement();

			var dropdownContentsElement = element.find('[dropdown-contents]');
			expect(dropdownContentsElement.css('visibility')).toBe('');

			var dropdownButton = element.find('[data-test="filter-selector-button"]');
			dropdownButton.click();
			scope.$digest();

			expect(dropdownContentsElement.css('visibility')).toBe('visible');
		});

		it('should re-apply a filter after clicking on the refresh icon', function () {
			var filterWatchTriggered = false;

			scope.myFilter = [diffConstants.rejected()];
			scope.myTotals = createTotalsObject(10, 5, 2);
			var element = createElement();

			scope.$watch('filter', function () {
				filterWatchTriggered = true;
			});

			element.find('[data-test="filter-reload-button"]').click();
			scope.$digest();
			expect(filterWatchTriggered).toBe(true);
			expect(scope.myFilter).toHaveSameItems([diffConstants.rejected()]);
		});

	});

	describe('the selection menu', function () {
		var menuElement;

		beforeEach(function () {
			scope.myFilter = diffConstants.all();
			scope.myTotals = createTotalsObject(10, 5, 2);
			var element = createElement();
			element.find('[data-test="filter-selector-button"]').click();

			menuElement = element.find('[dropdown-contents]');
		});

		it('should contain all filter options with the correct totals', function () {
			expect(menuElement.text()).toContain('all (17)');
			expect(menuElement.text()).toContain('only pending (2)');
			expect(menuElement.text()).toContain('only rejected (5)');
			expect(menuElement.text()).toContain('only accepted (10)');
			expect(menuElement.text()).toContain('only pending and rejected (7)');
		});

		it('should display updated totals for each option', function () {
			scope.myTotals = createTotalsObject(11, 6, 3);
			scope.$digest();
			expect(menuElement.text()).toContain('all (20)');
			expect(menuElement.text()).toContain('only pending (3)');
			expect(menuElement.text()).toContain('only rejected (6)');
			expect(menuElement.text()).toContain('only accepted (11)');
			expect(menuElement.text()).toContain('only pending and rejected (9)');
		});

		it('should update the filter scope value when an option is clicked', function () {
			expect(scope.myFilter).toHaveSameItems(diffConstants.all(), true);

			menuElement.find(':contains("only rejected")').click();
			expect(scope.myFilter).toHaveSameItems([diffConstants.rejected()], true);
		});

		it('should disable a diff option when its total is zero', function () {
			expect(menuElement.css('visibility')).toBe('visible');

			scope.myTotals = createTotalsObject(10, 5, 0);
			scope.$digest();

			var onlyPendingButton = menuElement.find('i:contains("only pending (0)")');
			var allButton = menuElement.find('i:contains("all (15)")');

			expect(onlyPendingButton.hasClass('diff-filter-disabled')).toBe(true);
			expect(allButton.hasClass('diff-filter-disabled')).toBe(false);

			onlyPendingButton.click();
			scope.$digest();
			expect(menuElement.css('visibility')).toBe('visible');

			allButton.click();
			scope.$digest();
			expect(menuElement.css('visibility')).toBe('hidden');
		});

		it('should close the menu when an option is clicked', function () {
			expect(menuElement.css('visibility')).toBe('visible');

			menuElement.find('i:contains("only rejected")').click();
			expect(menuElement.css('visibility')).toBe('hidden');
		});
	});
});
