'use strict';

// Use instrumented code for code coverage tests
var lib = process.env.LIB_COV ? 'lib-cov' : 'lib';

var sectery = require('../' + lib + '/sectery');
var client  = require('../lib/client');

sectery(client);

exports.sectery = {
  '@all': function(test) {
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
    client._message('testuser', '#test-channel', 'foo bar table flip baz');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, '╯°□°）╯︵ ┻━┻');
    test.done();
  },

  '@scala (help)': function(test) {
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

  '[pm] @echo': function(test) {
    test.deepEqual(client._channels, {});
    client._pm('testuser', 'testbot', '@echo ping');
    test.equal(client._lastSaid().from, 'testbot');
    test.equal(client._lastSaid().to, 'testuser');
    test.equal(client._lastSaid().message, 'ping');
    test.done();
  },

  '[pm] @setup - usage': function(test) {
    var message = "Usage: @setup <email|sms> <email@email.com|phone|code>";
    test.deepEqual(client._channels, {});
    client._pm('testuser', 'testbot', '@setup');
    test.equal(client._lastSaid().from, 'testbot');
    test.equal(client._lastSaid().to, 'testuser');
    test.equal(client._lastSaid().message,message);
    test.done();
  },
  '[pm] @setup - sms': function(test) {
    var message = "testuser, validation code sent! Check your texts.";
    test.deepEqual(client._channels, {});
    client._pm('testuser', 'testbot', '@setup sms 555-555-5555');
    test.equal(client._lastSaid().from, 'testbot');
    test.equal(client._lastSaid().to, 'testuser');
    test.equal(client._lastSaid().message,message);
    test.done();
  },
  '[pm] @setup - sms code': function(test) {
    var message = "testuser, code validated.";
    var uuid = require('../lib/utilities');
    var code = uuid.uuid();
    test.deepEqual(client._channels, {});
    client._pm('testuser', 'testbot', '@setup sms ' + code);
    test.equal(client._lastSaid().from, 'testbot');
    test.equal(client._lastSaid().to, 'testuser');
    test.equal(client._lastSaid().message,message);
    test.done();
  },
  '[pm] @setup - email': function(test) {
    var message = "testuser, validation code sent! Check your email.";
    test.deepEqual(client._channels, {});
    client._pm('testuser', 'testbot', '@setup email email@email.com');
    test.equal(client._lastSaid().from, 'testbot');
    test.equal(client._lastSaid().to, 'testuser');
    test.equal(client._lastSaid().message,message);
    test.done();
  },
  '[pm] @setup - email code': function(test) {
    var message = "testuser, code validated.";
    var uuid = require('../lib/utilities');
    var code = uuid.uuid();
    test.deepEqual(client._channels, {});
    client._pm('testuser', 'testbot', '@setup email ' + code);
    test.equal(client._lastSaid().from, 'testbot');
    test.equal(client._lastSaid().to, 'testuser');
    test.equal(client._lastSaid().message,message);
    test.done();
  },
};

