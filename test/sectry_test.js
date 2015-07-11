'use strict';

// Use instrumented code for code coverage tests
var lib = process.env.LIB_COV ? 'lib-cov' : 'lib';

var sectery   = require('../' + lib + '/sectery');
var client    = require('../lib/client');
var utilities = require('../lib/utilities');
var krypto = require('../lib/krypto-game');
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
    var message = "Usage: @setup <email|sms> <email@example.com|phone|code>";
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
    client._pm('testuser', 'testbot', '@setup email email@example.com');
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

  '@tell': function(test) {
    test.expect(4);

    client._message('testuser', '#test-channel', '@tell testuser1 Welcome back!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'I\'ll pass your message along.');

    client._message('testuser1', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'testuser1: testuser said "Welcome back!" at ' + utilities.now());

    test.done();
  },

  '@note': function(test) {
    test.expect(6);

    client._message('testuser', '#test-channel', '@note');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, "Usage: @note <message>");

    client._message('testuser', '#test-channel', '@note remind me to be reminded');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'I\'ll email you a reminder.');

    client._message('testuser1', '#test-channel', '@note remind me to be reminded');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'testuser1: PM me your email address with: /msg sectery @setup email name@example.com');

    test.done();
  },

  '@ascii art': function(test) {
    client._message('testuser', '#test-channel', '@ascii http://example.com/test.png');

    setTimeout(function() {
      test.expect(2);

      test.equal(client._lastSaid().to, '#test-channel');
      test.equal(client._lastSaid().message, "[ascii art]");

      test.done();
    }, 2000);
  },

  '@ascii text': function(test) {
    client._message('testuser', '#test-channel', '@ascii hello');

    setTimeout(function() {
      test.expect(1);

      var ascii_hello = [
        "                                ",
        ",--.            ,--.,--.        ",
        "|  ,---.  ,---. |  ||  | ,---.  ",
        "|  .-.  || .-. :|  ||  || .-. | ",
        "|  | |  |\\   --.|  ||  |' '-' ' ",
        "`--' `--' `----'`--'`--' `---'  ",
        "                                "
      ].join("\n");

      var message = client._said.splice(client._said.length - 7).map(function(reply) {
        return reply.message;
      }).join("\n");

      test.equal(message, ascii_hello);

      test.done();
    }, 4000);
  },

  '@simpsons': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', '@simpsons');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, "(S2E1): We have time for one more report. Bart Simpson? ");

    test.done();
  },
  '@cards': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', '@cards');
    test.equal(client._lastSaid().to, '#test-channel');
    var match = /^(\d+,?\s+){5}Objective\s+Card:\s+\d+$/.exec(client._lastSaid().message);
    test.ok(match, 'cards not found.');
    test.done();
  },
  '@krypto': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', '@krypto');
    test.equal(client._lastSaid().to, '#test-channel');
    var match = /testuser: OK - take a guess./.exec(client._lastSaid().message);
    test.ok(match, 'Not ready to guess');
    test.done();
    client._message('testuser', '#test-channel', '@guess 0');
  },
  '@krypto - wrong user': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', '@krypto');
    test.equal(client._lastSaid().to, '#test-channel');
    client._message('testuser1', '#test-channel', '@krypto');
    var match = /testuser1: sorry, but testuser already is guessing\./.exec(client._lastSaid().message);
    test.ok(match, 'Second user doesn\'t get failure message.');
    test.done();
    client._message('testuser', '#test-channel', '@guess 0');
  },
  '@guess - wrong user': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', '@krypto');
    test.equal(client._lastSaid().to, '#test-channel');
    client._message('testuser1', '#test-channel', '@guess');
    var match = /testuser1: sorry, but it\'s testuser\'s turn\./.exec(client._lastSaid().message);
    test.ok(match, 'Second user doesn\'t get failure message.');
    test.done();
    client._message('testuser', '#test-channel', '@guess 0');
  },
  '@guess - right user, wrong guess': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', '@krypto');
    test.equal(client._lastSaid().to, '#test-channel');
    client._message('testuser', '#test-channel', '@guess 0');
    var match = /testuser: Sorry, your answer is incorrect\./.exec(client._lastSaid().message);
    test.ok(match, 'User gets correct error message.');
    test.done();
  },
  '@guess - right user, right guess': function(test) {
    var k = new krypto.Krypto();
    k.hand = [[6],[4],[22],[6],[2],[1]];
    k.guesser = 'testuser';
    test.ok(k.checkSolution(k.guesser,'(6 + 4) / (22 - 6 * 2)'),'check solution is false');
    test.done();
  },
  '@time': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', '@time');
    test.equal(client._lastSaid().to, '#test-channel');

    var match = new RegExp('.*, (\\d+ days?)? ?(\\d+ hours?)? ?(\\d+ minutes?)? ?(\\d+ seconds?)? ?until end of next workday\\.');
    
    var match = match.exec(client._lastSaid().message);
    test.ok(match, 'Incorrect time format');
    test.done();
  },
  '@remind-before': function(test) {
    test.expect(2);

    var msg = '@remind 2 test';
    client._message('testuser', '#test-channel',msg);
    var now = utilities.now();

    setTimeout(function() {
      test.expect(2);

      test.equal(client._lastSaid().to, '#test-channel');
      test.equal(client._lastSaid().message,msg);
      test.done();
    },1000);

  },
  '@remind-after': function(test) {
    test.expect(2);

    var msg = '@remind 2 test';
    client._message('testuser', '#test-channel',msg);
    var now = utilities.now();

    setTimeout(function() {
      test.expect(2);

      test.equal(client._lastSaid().to, '#test-channel');
      test.equal(client._lastSaid().message,'testuser: Reminder: test ' + now);
      test.done();
    },2000);
  },
};

