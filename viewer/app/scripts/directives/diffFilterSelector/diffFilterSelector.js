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
	.directive('diffFilterSelector', function (diffConstants) {
		return {
			restrict: 'AE',
			scope: {
				filter: '=diffFilterSelector',
				totals: '='
			},
			templateUrl: 'scripts/directives/diffFilterSelector/diffFilterSelector.html',
			link: function (scope) {
				scope.filterOptions = [
					{label: 'all', filter: [diffConstants.pending(), diffConstants.rejected(), diffConstants.accepted()], count: function () { return scope.totals.all; }},
					{label: 'only pending', filter: [diffConstants.pending()], count: function () { return scope.totals.pending; }},
					{label: 'only rejected', filter: [diffConstants.rejected()], count: function () { return scope.totals.rejected; }},
					{label: 'only accepted', filter: [diffConstants.accepted()], count: function () { return scope.totals.accepted }},
					{label: 'only pending and rejected', filter: [diffConstants.pending(), diffConstants.rejected()], count: function () { return scope.totals.pending + scope.totals.rejected; }}
				];

				scope.selectFilterOption = function (newOption) {
					if (newOption.count() > 0) {
						scope.filterSelection = newOption;
					}
				};

				scope.reapplyFilter = function () {
					// forces triggering $watch
					scope.filterSelection = angular.copy(scope.filterSelection);
				};

				scope.updateFilterSelection = function () {
					scope.filterSelection = _.find(scope.filterOptions, function (anOption) {
							return _.isEqual(anOption.filter, scope.filter);
						}) || scope.filterOptions[0];
				};

				scope.$watch('filterSelection', function (newOption) {
					scope.filter = newOption.filter;
				});

				scope.$watch('filter', function () {
					scope.updateFilterSelection();
				});

				scope.updateFilterSelection();
			}
		};
	});