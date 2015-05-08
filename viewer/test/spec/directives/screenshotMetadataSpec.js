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

describe('Directive: screenshot-metadata', function () {

	beforeEach(module('visualDiffViewerApp'));

	var	scope,
		$compile,
		element;

	beforeEach(inject(function ($rootScope, _$compile_) {
		$compile = _$compile_;

		scope = $rootScope.$new();
		scope.myProperties = {};
		scope.myMeta = {};

		element = compileAngularHtml('<div screenshot-metadata meta="myMeta" properties="myProperties"></div>', scope, $compile);
	}));

	it('should not throw any errors when both metadata and properties are empty or undefined', function() {
		scope.myProperties = undefined;
		scope.myMeta = undefined;
		scope.$digest();
	});

	it('should render os, browser, version and resolution data based on both meta and properties attribute values', function() {
		scope.myProperties = {os: 'MAC', browser: 'firefox', version: '30.1', resolution: '800x600'};
		scope.$digest();

		var getTitle = function () { return element.find('span[title]').attr('title') };
		expect(getTitle()).toBe('MAC - firefox 30.1 - 800x600');

		scope.myProperties = {};
		scope.myMeta = {os: 'windows', browser: 'chrome', version: '40.11', resolution: '640x480'};
		scope.$digest();
		expect(getTitle()).toBe('windows - chrome 40.11 - 640x480');

		scope.myProperties = {version: '8.0', os: 'linux'};
		scope.myMeta = {browser: 'konqueror', resolution: '1024x768'};
		scope.$digest();
		expect(getTitle()).toBe('linux - konqueror 8.0 - 1024x768');
		expect(element.text()).toContain("8.0");
		expect(element.text()).toContain("1024x768");
	});

	it('should render os icon for known values, a text value for unknown osses', function() {
		scope.myProperties = {os: 'linux'};
		scope.$digest();

		expect(element.find('.icon-os-linux').length).toBe(1);
		expect(element.find('.icon-os-windows').length).toBe(0);

		scope.myProperties = {os: 'windows'};
		scope.$digest();
		expect(element.find('.icon-os-windows-old').length).toBe(1);
		expect(element.text()).not.toContain('windows');

		scope.myProperties = {os: 'win8'};
		scope.$digest();
		expect(element.find('.icon-os-windows').length).toBe(1);
		expect(element.text()).not.toContain('win8');

		scope.myProperties = {os: 'mac'};
		scope.$digest();
		expect(element.find('.icon-os-mac').length).toBe(1);
		expect(element.text()).not.toContain('mac');

		scope.myProperties = {os: 'android'};
		scope.$digest();
		expect(element.find('.icon-os-android').length).toBe(1);
		expect(element.text()).not.toContain('android');

		scope.myProperties = {os: 'os2'};
		scope.$digest();
		expect(element.text()).toContain('os2');
	});

	it('should render browser icon for known values, a text value for unknown browsers', function() {
		scope.myProperties = {browser: 'firefox'};
		scope.$digest();

		expect(element.find('.icon-browser-firefox').length).toBe(1);
		expect(element.find('.icon-browser-chrome').length).toBe(0);

		scope.myProperties = {browser: 'chrome'};
		scope.$digest();
		expect(element.find('.icon-browser-chrome').length).toBe(1);
		expect(element.find('.icon-browser-firefox').length).toBe(0);
		expect(element.text()).not.toContain('chrome');

		scope.myProperties = {browser: 'internet explorer'};
		scope.$digest();
		expect(element.find('.icon-browser-ie').length).toBe(1);

		scope.myProperties = {browser: 'safari'};
		scope.$digest();
		expect(element.find('.icon-browser-safari').length).toBe(1);

		scope.myProperties = {browser: 'konqueror'};
		scope.$digest();
		expect(element.text()).toContain('konqueror');
	});

});
