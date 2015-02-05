'use strict';

describe('Directive: keyboard', function () {

  // load the directive's module
  beforeEach(module('visualDiffViewerApp'));

  var element,
    scope,
    $document;

  beforeEach(inject(function ($rootScope, _$document_) {
    scope = $rootScope.$new();
    $document = _$document_;
  }));

  it('should broadcast scope event on keydown on document', inject(function ($compile) {
    var keydownTriggered = false;
    scope.$on('keydown', function(e) {
      keydownTriggered = true;
    });

    element = angular.element('<div keyboard></div>');
    element = $compile(element)(scope);

    var e = $.Event("keydown");
    e.which = 40;

    $document.trigger(e);
    scope.$digest();

    expect(keydownTriggered).toBe(true);
  }));
});
