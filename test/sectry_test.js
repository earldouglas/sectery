'use strict';

var sectery   = require('../lib/sectery');
var utilities = require('../lib/utilities');
var krypto    = require('../lib/krypto-game');
var irc       = require('irc');

var assert   = require('assert');

function user() {

  var client = new irc.Client(
    'irc.freenode.net',
    'sectery|test',
    {
      password: process.env.IRC_PASS,
      channels: ['#sectery'],
    }
  );

  client.addListener('error', function (message) {
    console.log(client.nick, 'error', messsage);
  });

  var expectM = function (done, expectedFrom, messagePredicate) {
    var listener =
      function (from, to, message) {
        if (expectedFrom === from && messagePredicate(message)) {
          client.removeListener('message', listener);
          done();
        }
      }; client.addListener('message', listener);
  };

  return {
    nick: function () { return client.nick; },
    expectM: expectM,
    expectMessage: function (done, expectedFrom, expectedMessage) {
      return expectM(done, expectedFrom, function (x) {
        return x === expectedMessage;
      });
    },
    expectMessageR: function (done, expectedFrom, expectedMessageR) {
      return expectM(done, expectedFrom, function (x) {
        return expectedMessageR.test(x);
      });
    },
    expectPM: function (done, expectedFrom, expectedMessage) {
      var listener =
        function (from, message) {
          if (expectedFrom === from && expectedMessage === message) {
            client.removeListener('pm', listener);
            done();
          }
        };
      client.addListener('pm', listener);
    },
    client: client,
    message: function (x) {
      client.say('#sectery', x);
    },
    privateMessage: function (x) {
      client.say(secteryUser.nick(), x);
    },
    part: function (k) {
      client.part('#sectery', k);
    },
    join: function (k) {
      client.join('#sectery', k);
    },
  };

}

var secteryUser = user();
var testUser = user();
var testUser2 = user();

sectery(secteryUser.client);

describe('sectery', function () {
  this.timeout(60000);

  it('should wait for sectery to join', function (done) {
    var joinListener = function (channel, nick, message) {
      if (nick === secteryUser.nick()) {
        process.env.IRC_USER = nick;
        secteryUser.client.removeListener('join', joinListener);
        done();
      } 

    };
    secteryUser.client.addListener('join', joinListener);
  });

  it('should greet user upon joining', function (done) {
    testUser.part(function (nick) {
      testUser.expectMessage(done, secteryUser.nick(),
        'Hey, ' + testUser.nick() + '!');
      testUser.join('#sectery');
    });
  });

  it('@all', function (done) {
    testUser.expectM(done, secteryUser.nick(), function (x) {
      var substr = testUser.nick();
      return x.indexOf(substr >= 0);
    });
    testUser.message('@all');
  });

  it('emoji', function(done) {
    testUser.expectMessage(done, secteryUser.nick(), '╯°□°）╯︵ ┻━┻');
    testUser.message('foo bar table flip baz');
  });

  it('html title with numeric http code(s)', function(done) {
    testUser.expectMessage(done, secteryUser.nick(),
      'é HTML Entity code in title tags - Stack Overflow');
    testUser.message('http://stackoverflow.com/questions/11037123/' +
      '%C3%A9-html-entity-code-in-title-tags');
  });

  it('html title', function(done) {
    testUser.expectMessage(done, secteryUser.nick(), 'James Earl Douglas');
    testUser.message('http://earldouglas.com/');
  });

  it('@poll (ls)', function(done) {

    testUser.expectM(done, secteryUser.nick(), function (x) {
      var regex = new RegExp('Usage: @poll <start|close> <message>');
      return regex.test(x);
    });
    var command = '@poll';
    testUser.message(command);
  });

  it('@poll (add)', function(done) {
    testUser.expectM(done, secteryUser.nick(), function (x) {
      var regex = new RegExp(testUser.nick().replace(/[|]/g, '\\|') + ': OK - Poll \\d+ started!');
      return regex.test(x);
    });
    var command = '@poll start Is this a poll?';
    testUser.message(command);
  });


  it('@poll (close) - wrong user', function(done) {
    testUser.expectM(done, secteryUser.nick(), function (x) {
      var regex = new RegExp(testUser2.nick().replace(/[|]/g, '\\|') + ': Sorry - Poll \\d+ can only be closed by "' +  testUser.nick().replace(/[|]/g, '\\|') + '"!');
      return regex.test(x);
    });
    var command = '@poll close 0';
    testUser2.message(command);
  });

  it('@poll (close) - wrong id', function(done) {
    testUser.expectM(done, secteryUser.nick(), function (x) {
      var regex = new RegExp(testUser.nick().replace(/[|]/g, '\\|') + ': Sorry - Poll \\d+ was not found.');
      return regex.test(x);
    });
    var command = '@poll close 2';
    testUser.message(command);
  });
  it('@poll (close)', function(done) {
    testUser.expectM(done, secteryUser.nick(), function (x) {
      var regex = new RegExp(testUser.nick().replace(/[|]/g, '\\|') + ': OK - Poll \\d+ closed!');
      return regex.test(x);
    });
    var command = '@poll close 0';
    testUser.message(command);
  });

  it('@poll (closed)', function(done) {
    testUser.expectM(done, secteryUser.nick(), function (x) {
      var regex = new RegExp(testUser.nick().replace(/[|]/g, '\\|') + ': Sorry - Poll \\d+ is already closed!');
      return regex.test(x);
    });
    var command = '@poll close 0';
    testUser.message(command);
  });
  it('@poll (ls)', function(done) {

    testUser.expectM(done, secteryUser.nick(), function (x) {
      var regex = new RegExp('Usage: @poll <start|close> <message>');
      return regex.test(x);
    });
    var command = '@poll';
    testUser.message(command);
  });
});
