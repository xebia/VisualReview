describe('The run controller', function() {
  var $scope,
      ctrl,
      TitleService;

  var RUN_ID = 3;

  beforeEach(function (){
    TitleService = jasmine.createSpyObj('TitleService', ['setTitle']);

    module('visualDiffViewerApp');

    inject(function($rootScope, $controller) {
      $scope = $rootScope.$new();

      ctrl = $controller('RunCtrl', {
        $scope: $scope,
        $routeParams: {
          runId: RUN_ID
        },
        TitleService: TitleService
      });
    });
  });

  it('should set default variables on scope', function() {
    expect($scope.selectedDiffIndex).toEqual(0);
    expect($scope.selectedScreenshot).toEqual('after');
    expect($scope.showDiff).toEqual(true);
  });

  it('should set the page title', function () {
    expect(TitleService.setTitle).toHaveBeenCalledWith('Run ' + RUN_ID);
  });

});
