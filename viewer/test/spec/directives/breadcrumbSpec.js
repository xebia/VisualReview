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

describe('Directive: breadcrumb', function () {

	beforeEach(module('visualDiffViewerApp'));

	var	scope,
			$compile;

	function compileHtml(html, scope) {
		var element = angular.element(html);
		element = $compile(element)(scope);
		scope.$digest();
		return element;
	}

	function testHyperLink(element, index, href, text) {
		var link = element.find('a:eq(' + index + ')');
		expect(link.attr('href')).toBe(href);
		expect(link.text()).toBe(text);
	}

	beforeEach(inject(function ($rootScope, _$compile_) {
		scope = $rootScope.$new();
		scope.myProjectId = '1';
		scope.myProjectName = 'myProject';
		scope.mySuiteId = '2';
		scope.mySuiteName = 'mySuite';
		scope.myRunId = '3';
		scope.myCreationTime = new Date(2010, 1, 1, 12, 13, 14, 0);

		$compile = _$compile_;
	}));

	it('should not render anything when not enough project data attributes are given', function() {
		var element = compileHtml('<div breadcrumb></div>', scope, $compile);
		expect(element.children().size()).toBe(0);

		element = compileHtml('<div breadcrumb project-id="myProjectId"></div>', scope, $compile);
		expect(element.children().size()).toBe(0);

		element = compileHtml('<div breadcrumb project-name="myProjectName"></div>', scope, $compile);
		expect(element.children().size()).toBe(0);
	});

	it('should only display a link to the project when project data is given', function () {
		var element = compileHtml('<div breadcrumb project-id="myProjectId" project-name="myProjectName"></div>', scope, $compile);

		testHyperLink(element, 0, '#/1/', 'myProject');
	});

	it('should display a link to the project and suite when project- and suite data are given', function () {
		var element = compileHtml('<div breadcrumb ' +
		'project-id="myProjectId" project-name="myProjectName"' +
		'suite-id="mySuiteId" suite-name="mySuiteName"' +
		'></div>', scope, $compile);

		expect(element.find('a').length).toBe(2);
		testHyperLink(element, 0, '#/1/', 'myProject');
		testHyperLink(element, 1, '#/1/2', 'mySuite');
	});

	it('should display a link to the project, suite and run when project-, suite- and run data are given', function () {
		var element = compileHtml('<div breadcrumb ' +
		'project-id="myProjectId" project-name="myProjectName" ' +
		'suite-id="mySuiteId" suite-name="mySuiteName" ' +
		'run-id="myRunId" creation-time="myCreationTime" ' +
		'></div>', scope, $compile);

		expect(element.find('a').length).toBe(3);

		testHyperLink(element, 0, '#/1/', 'myProject');
		testHyperLink(element, 1, '#/1/2', 'mySuite');
		testHyperLink(element, 2, '#/1/2/3', '3 (Feb 1, 2010 at 12:13PM)');
	});

	it('should update the link to project when the scope value is updated', function () {
		var element = compileHtml('<div breadcrumb ' +
		'project-id="myProjectId" project-name="myProjectName" ' +
		'suite-id="mySuiteId" suite-name="mySuiteName" ' +
		'run-id="myRunId" creation-time="myCreationTime" ' +
		'></div>', scope, $compile);

		scope.myProjectId = '11';
		scope.myProjectName = 'myProject2';
		scope.mySuiteId = '22';
		scope.mySuiteName = 'mySuite2';
		scope.myRunId = '33';
		scope.myCreationTime = new Date(2011, 1, 1, 12, 13, 14, 0);
		scope.$digest();

		expect(element.find('a').length).toBe(3);

		testHyperLink(element, 0, '#/11/', 'myProject2');
		testHyperLink(element, 1, '#/11/22', 'mySuite2');
		testHyperLink(element, 2, '#/11/22/33', '33 (Feb 1, 2011 at 12:13PM)');
	});
});
