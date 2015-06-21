module.exports = function () {
  function n(file) { return { pattern: file, instrument: false }; }

  return {
    files: [
      // Libraries
      n('app/bower_components/jquery/jquery.js'),
      n('app/bower_components/underscore/underscore.js'),
      n('app/bower_components/angular/angular.js'),
      n('app/bower_components/angular-mocks/angular-mocks.js'),
      n('app/bower_components/angular-resource/angular-resource.js'),
      n('app/bower_components/angular-route/angular-route.js'),
      n('app/bower_components/angular-cookies/angular-cookies.js'),
      n('app/bower_components/angular-animate/angular-animate.js'),
      n('app/bower_components/angular-sanitize/angular-sanitize.js'),
      n('node_modules/jasmine-collection-matchers/lib/pack.js'),

      // VR core
      'app/scripts/**/*.js',
      'app/scripts/**/*.html',

      // Test utilities
      'test/spec/test_util/test_util.js'
    ],

    tests: [
      'test/spec/**/*.js',
      { pattern: 'test/spec/test_util/test_util.js', ignore: true}
    ],

    preprocessors: {
      'app/scripts/**/*.html': function (file) {
        return require('wallaby-ng-html2js-preprocessor').transform(file, {
          stripPrefix: 'app/',
          moduleName: 'visualDiffViewerApp'
        });
      }
    }
  };

};
