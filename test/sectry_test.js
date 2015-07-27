'use strict';

var sectery   = require('../lib/sectery');
var utilities = require('../lib/utilities');
var krypto    = require('../lib/krypto-game');
var irc       = require('irc');

var assert   = require('assert');

function user(suffix) {

  var nick = 'sectery|' + suffix;

  var client = new irc.Client(
    'irc.freenode.net',
    nick,
    {
      password: process.env.IRC_PASS,
      channels: ['#sectery'],
    }
  );

  client.addListener('error', function (message) {
    console.log(suffix, 'error', messsage);
  });

  var expectM = function (done, expectedFrom, messagePredicate) {
    var listener =
      function (from, to, message) {
        if (expectedFrom === from && messagePredicate(message)) {
          client.removeListener('message', listener);
          done();
        }
      };
    client.addListener('message', listener);
  };

  return {
    nick: nick,
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
      client.say(secteryUser.nick, x);
    },
    part: function (k) {
      client.part('#sectery', k);
    },
    join: function (k) {
      client.join('#sectery', k);
    },
  };

}

var secteryUser = user('test');
var testUser = user('test1');
var testUser2 = user('test2');

process.env.IRC_USER = secteryUser.nick;
var replyInterval = sectery(secteryUser.client);

describe('sectery', function () {
  this.timeout(60000);

  it('should wait for sectery to join', function (done) {
    var joinListener = function (channel, nick, message) {
      if (nick === secteryUser.nick) {
        secteryUser.client.removeListener('join', joinListener);
        done();
      }
    };
    secteryUser.client.addListener('join', joinListener);
  });

  it('should greet user upon joining', function (done) {
    testUser.part(function (nick) {
      testUser.expectMessage(done, secteryUser.nick, 'Hey, ' + testUser.nick + '!');
      testUser.join('#sectery');
    });
  });

  it('@all', function (done) {
    testUser.expectM(done, secteryUser.nick, function (x) {
      var substr = testUser.nick;
      return x.indexOf(substr >= 0);
    });
    testUser.message('@all');
  });

  it('emoji', function(done) {
    testUser.expectMessage(done, secteryUser.nick, '╯°□°）╯︵ ┻━┻');
    testUser.message('foo bar table flip baz');
  });

  it('@scala (help)', function(done) {
    testUser.expectMessage(done, secteryUser.nick, 'Usage: @scala <expression>');
    testUser.message('@scala');
  });

  it.skip('@scala', function(done) {
    testUser.expectMessage(done, secteryUser.nick, 'res0: Int = 42');
    testUser.message('@scala 6 * 7');
  });

  it('html title with numeric http code(s)', function(done) {
    testUser.expectMessage(done, secteryUser.nick,
      'é HTML Entity code in title tags - Stack Overflow');
    testUser.message('http://stackoverflow.com/questions/11037123/' +
      '%C3%A9-html-entity-code-in-title-tags');
  });

  it('html title', function(done) {
    testUser.expectMessage(done, secteryUser.nick, 'James Earl Douglas');
    testUser.message('http://earldouglas.com/');
  });

  it('[pm] @echo', function(done) {
    testUser.expectPM(done, secteryUser.nick, 'ping');
    testUser.privateMessage('@echo ping');
  });

  it('[pm] @setup - usage', function(done) {
    var message = "Usage: @setup <email|sms> <email@example.com|phone|code>";
    testUser.expectPM(done, secteryUser.nick, message);
    testUser.privateMessage('@setup');
  });

  it('@tell (set)', function(done) {
    testUser.expectMessage(done, secteryUser.nick, "I'll pass your message along.");
    testUser.message('@tell ' + testUser2.nick + ' Welcome back!');
  });

  it('@tell (get)', function(done) {
    testUser2.part(function (nick) {
      testUser2.join(function (nick) {
        testUser2.expectMessageR(done, secteryUser.nick,
          new RegExp(testUser2.nick + ': ' + testUser.nick +
            ' said "Welcome back!"'));
        testUser2.message('Hey, everyone!');
      });
    });
  });

  it('@note (usage)', function(done) {
    testUser.expectMessage(done, secteryUser.nick, 'Usage: @note <message>');
    testUser.message('@note');
  });

  it('@note (no email)', function(done) {
    testUser.expectMessage(done, secteryUser.nick, testUser.nick +
      ': PM me your email address with: /msg ' +
      secteryUser.nick + ' @setup email name@example.com');
    testUser.message('@note Testing is hard.');
  });

  it.skip('@ascii art', function(done) {
    testUser.expectMessage(done, secteryUser.nick, '[ascii art]');
    testUser.message('@ascii http://example.com/test.png');
  });

  it('@simpsons', function(done) {
    testUser.expectMessageR(done, secteryUser.nick, /^\(S\d+E\d+\): /);
    testUser.message('@simpsons');
  });

  it('@cards', function (done) {
    testUser.expectMessageR(done, secteryUser.nick,
      /^(\d+,?\s+){5}Objective\s+Card:\s+\d+$/);
    testUser.message('@cards');
  });

  it('@krypto (premature @guess)', function(done) {
    testUser.expectMessage(done, secteryUser.nick,
      testUser.nick + ': please say "@krypto" first!');
    testUser.message('@guess');
  });

  it('@krypto', function(done) {
    testUser.expectMessage(done, secteryUser.nick,
      testUser.nick + ': OK - take a guess.');
    testUser.message('@krypto');
  });

  it('@krypto (wrong user)', function(done) {
    testUser2.expectMessage(done, secteryUser.nick,
      testUser2.nick + ": sorry, but it's " + testUser.nick + "'s turn.");
    testUser2.message('@guess');
  });
  it('@cron (usage)', function(done) {
    testUser2.expectMessage(done, secteryUser.nick,
                            'Usage: @cron <add|remove|ls> "<cron-string>" <message>|<id>');
    testUser2.message('@cron');
  });

  it('@cron (bogus input)', function(done) {
    testUser2.expectMessage(done, secteryUser.nick,
                            'Error: '+ testUser2.nick + ': The cron string "bogus input" is not valid. Usage: @cron <add|remove|ls> "<cron-string>" <message>|<id>');
    testUser2.message('@cron add "bogus input" test');
  });

  it('@cron (remove-not found)', function(done) {
    testUser2.expectMessage(done, secteryUser.nick,
                            'Error: '+ testUser2.nick + ': The cron job with id "1" was not found. Usage: @cron <add|remove|ls> "<cron-string>" <message>|<id>');
    testUser2.message('@cron remove 1');
  });
  it('@cron (add)', function(done) {
    testUser2.expectMessage(done, secteryUser.nick,
                            testUser2.nick + ': OK - cron job 0 scheduled!');
    testUser2.message('@cron add "* * * * * *" This is cool. ');
    testUser2.message('@cron remove 0');
  });
});
    
//    
//    client._message(user1, '#test-channel', '@cron add "10 * * * * *" This is cool.');
//    test.equal(client._lastSaid().to, '#test-channel');
//    test.equal(client._lastSaid().message, user1 + ': OK - cron job 1 scheduled!');
//
//    setTimeout(function() {
//      var expected = "This is cool. This is cool.";
//      var message = client._said.splice(client._said.length - 2).map(function(reply) {
//        return reply.message;
//      }).join(" ");
//      test.equal(message, expected);
//
//      client._message(user1, '#test-channel', '@cron remove 0');
//      test.equal(client._lastSaid().to, '#test-channel');
//      test.equal(client._lastSaid().message, user1 + ': OK - cron job 0 stopped!' );
//
//      client._message(user1, '#test-channel', '@cron remove 1');
//      test.equal(client._lastSaid().to, '#test-channel');
//      test.equal(client._lastSaid().message, user1 + ': OK - cron job 1 stopped!' );
//
//      test.done();
//    }, 3000);
//
//  },
//  '@cron [ls]': function(test) {
//    test.expect(8);
//    
//    var user1 = Math.random().toString(36).substr(2, 5);
//    client._message(user1, '#test-channel', '@cron');
//    test.equal(client._lastSaid().to, '#test-channel');
//    test.equal(client._lastSaid().message, 'Usage: @cron <add|remove|ls> "<cron-string>" <message>|<id>' );
//
//    client._message(user1, '#test-channel', '@cron add "20 * * * * *" This is cool.');
//    test.equal(client._lastSaid().to, '#test-channel');
//    test.equal(client._lastSaid().message, user1 + ': OK - cron job 2 scheduled!');
//    
//    client._message(user1, '#test-channel', '@cron add "10 * * * * *" This is cool.');
//    test.equal(client._lastSaid().to, '#test-channel');
//    test.equal(client._lastSaid().message, user1 + ': OK - cron job 3 scheduled!');
//
//    var then = utilities.now();
//    client._message(user1, '#test-channel', '@cron ls');
//    test.equal(client._lastSaid().to, '#test-channel');
//
//    setTimeout(function() {
//      var message = client._said.splice(client._said.length - 2).map(function(reply) {
//        return reply.message;
//      }).join(" ");
//      var expected = '2: "20 * * * * *" "This is cool." ' + then + " " + '3: "10 * * * * *" "This is cool." ' + then;
//      test.equal(message, expected);
//
//      client._message(user1, '#test-channel', '@cron remove 2');
//      client._message(user1, '#test-channel', '@cron remove 3');
//      test.done();
//    }, 500);
//  },
//  '@cron [cmd]': function(test) {
//    test.expect(6);
//    
//    var user1 = Math.random().toString(36).substr(2, 5);
//    client._message(user1, '#test-channel', '@cron add "* * * * * *" @simpsons');
//    test.equal(client._lastSaid().to, '#test-channel');
//    test.equal(client._lastSaid().message, user1 + ': OK - cron job 4 scheduled!');
//    
//    setTimeout(function() {
//      test.equal(client._lastSaid().to, '#test-channel');
//      test.equal(client._lastSaid().message, "(S2E1): We have time for one more report. Bart Simpson? ");
//
//      client._message(user1, '#test-channel', '@cron remove 4');
//      test.equal(client._lastSaid().to, '#test-channel');
//      test.equal(client._lastSaid().message, user1 + ': OK - cron job 4 stopped!' );
//      test.done();
//    }, 2000);
//
//  },
//};
//
