'use strict';

// Use instrumented code for code coverage tests
var lib = process.env.LIB_COV ? 'lib-cov' : 'lib';

var sectery = require('../' + lib + '/sectery');
var client  = require('./mock-client')('testbot');

sectery(client);

exports.sectery = {
  '@all': function(test) {
    test.expect(4);

    client._join('#test-channel','fred',"what up?");
    test.equal(client._lastSaid().message, 'Hey, fred!');

    client._join('#test-channel','testuser',"what up?");
    test.equal(client._lastSaid().message, 'Hey, testuser!');

    client._join('#test-channel','bob',"yo");
    client._join('#test-channel','foo',"doh");
    client._part('#test-channel','bob','i-don\'t-know',"yo");

    client._message('testuser', '#test-channel', '@all');
    test.equal(client._lastSaid().message, 'foo, fred, testuser');

    client._join('#test-channel','bob',"yo");

    client._message('testuser', '#test-channel', '@all');
    test.equal(client._lastSaid().message, 'bob, foo, fred, testuser');

    // clean up
    client._part('#test-channel','fred','i-don\'t-know',"yo");
    client._part('#test-channel','testuser','i-don\'t-know',"yo");
    client._part('#test-channel','foo','i-don\'t-know',"yo");
    client._part('#test-channel','bob','i-don\'t-know',"yo");

    test.done();
  },

  'emoji': function(test) {
    test.expect(2);
    client._message('testuser', '#test-channel', 'foo bar table flip baz');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, '╯°□°）╯︵ ┻━┻');
    test.done();
  },

  '@scala (help)': function(test) {
    test.expect(2);
    client._message('testuser', '#test-channel', '@scala');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, "Usage: @scala <expression>");
    test.done();
  },

  '@scala': function(test) {

    client._message('testuser', '#test-channel', '@scala 2 + 3');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal('res0: Int = 5', client._lastSaid().message);

    client._message('testuser', '#test-channel', '@scala 5 + 7');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal('res1: Int = 12', client._lastSaid().message);

    test.done();
  },

  'html title with numeric http code(s)': function(test) {
    client._message('testuser', '#test-channel', 'http://stackoverflow.com/questions/11037123/%C3%A9-html-entity-code-in-title-tags');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'é HTML Entity code in title tags - Stack Overflow');
    test.done();
  },

  'html title with no title': function(test) {
    client._message('testuser', '#test-channel', 'https://www.google.com/images/srpr/logo11w.png');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'https://www.google.com/images/srpr/logo11w.png');
    test.done();
  },

  'html title': function(test) {
    client._message('testuser', '#test-channel', 'https://www.google.com/');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'Google');
    test.done();
  },

};
