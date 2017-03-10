'use strict';

var sectery   = require('../lib/sectery');
var utilities = require('../lib/utilities');
var krypto    = require('../lib/krypto-game');
var utilities = require('../lib/utilities');

var assert   = require('assert');

process.env.IRC_USER = 'sectery-test';

describe('pm listeners', function () {

  var test = function (name, req, res) {
    it(name, function () {
      var listener = require('../lib/listeners/pm/' + name + '.js');
      assert.deepEqual(listener(req.db, req.from, req.message), res.messages);
      assert.deepEqual(req.db, res.db);
    });
  };

  test('echo',
    { db: {}, channel: '#test-channel', from: 'test-user', message: '@echo ping' },
    {
      db: {}, messages: [ { message: 'ping', to: 'test-user' } ]
    }
  );

  test('setup',
    { db: {}, channel: '#test-channel', from: 'test-user', message: '@setup' },
    {
      db: {}, messages: [ { message: 'Usage: @setup <email|sms> <email@example.com|phone|code>', to: 'test-user' } ]
    }
  );

});

describe('join listeners', function () {

  var test = function (name, req, res) {
    it(name, function () {
      var listener = require('../lib/listeners/join/' + name + '.js');
      assert.deepEqual(listener(req.db, req.channel, req.nick, req.message), res.messages);
      assert.deepEqual(req.db, res.db);
    });
  };

  test('default',
    { db: {}, channel: '#test-channel', nick: 'test-user', message: '' },
    {
      db: { nicks: { '#test-channel': { 'test-user': true } } },
      messages: [ { message: 'Hey, test-user!', to: '#test-channel' } ]
    }
  );

});

describe('part listeners', function () {

  var test = function (name, req, res) {
    it(name, function () {
      var listener = require('../lib/listeners/part/' + name + '.js');
      assert.deepEqual(listener(req.db, req.channel, req.nick, req.reason, req.message), res.messages);
      assert.deepEqual(req.db, res.db);
    });
  };

  test('default',
    {
      db: { nicks: { '#test-channel': { 'test-user': true } } },
      channel: '#test-channel', nick: 'test-user', reason: '', message: ''
    },
    {
      db: { nicks: { '#test-channel': {} } },
      messages: undefined
    }
  );

});

describe('message listeners', function () {

  var test = function (name, req, res) {
    it(name, function () {
      this.timeout(10000);
      var listener = require('../lib/listeners/message/' + name + '.js');
      assert.deepEqual(listener(req.db, req.from, req.channel, req.message), res.messages);
      assert.deepEqual(req.db, res.db);
    });
  };

  var testR = function (name, req, res) {
    it(name, function () {
      this.timeout(10000);
      var listener = require('../lib/listeners/message/' + name + '.js');
      var messages = listener(req.db, req.user, req.channel, req.message);
      assert.equal(messages.length, res.messages.length);
      for (var i = 0; i < res.messages.length; i++) {
        assert.equal(messages[i].to, res.messages[i].to);
        assert.equal(res.messages[i].message.test(messages[i].message), true);
      }
      assert.deepEqual(req.db, res.db);
    });
  };

  var testIO = function (name, input, output) {
    test(name,
      { db: {}, from: 'test-user', channel: '#test-channel', message: input },
      { db: {}, messages: [ { message: output, to: '#test-channel' } ] }
    );
  };

  test('all',
    {
      db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } } },
      from: 'test-user', channel: '#test-channel', message: '@all'
    },
    {
      db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } } },
      messages: [ { message: 'test-user, test-user-2', to: '#test-channel' } ]
    }
  );

  testIO('weather', '@weather', '@weather <location>');

  testR('weather',
    { db: {}, from: 'test-user', channel: '#test-channel', message: '@weather Boulder' },
    { db: {},
      messages: [
        { message: /^ /,          to: '#test-channel' },
        { message: / \u00b0F *$/, to: '#test-channel' },
        { message: / mph *$/,     to: '#test-channel' },
        { message: / mi *$/,      to: '#test-channel' },
        { message: / in *$/,      to: '#test-channel' },
      ]
    }
  );

  var everyDayDb = function (n) {
    return {
      replies: {
	'#test-channel': [
	  {
	    count: n,
	    flags: '',
	    name: 'every-day',
	    regex: 'everyday',
	    reply: 'EVERYDAY',
	  }
	]
      }
    };
  };

  test('auto-reply',
    {
      db: {},
      from: 'test-user', channel: '#test-channel',
      message: '@reply every-day /everyday/ EVERYDAY'
    },
    {
      db: everyDayDb(10),
      from: 'test-user', channel: '#test-channel',
      messages: [ { message: 'test-user: OK - auto-reply "every-day" added.', to: '#test-channel' } ]
    }
  );

  test('auto-reply',
    {
      db: everyDayDb(10),
      from: 'test-user', channel: '#test-channel', message: '@reply'
    },
    {
      db: everyDayDb(10),
      messages: [
        { message: '@reply <name> /<regex>/[ig] <reply>', to: '#test-channel' },
        { message: '@reply delete <name>', to: '#test-channel' },
        { message: 'Replies: every-day', to: '#test-channel' },
      ]
    }
  );

  test('auto-reply',
    {
      db: everyDayDb(10),
      from: 'test-user', channel: '#test-channel', message: 'everyday'
    },
    {
      db: everyDayDb(9),
      messages: [ { message: 'EVERYDAY', to: '#test-channel' }, ]
    }
  );

  test('auto-reply',
    {
      db: everyDayDb(1),
      from: 'test-user', channel: '#test-channel', message: 'everyday'
    },
    {
      db: { replies: { '#test-channel': [] } },
      messages: [ { message: 'EVERYDAY', to: '#test-channel' }, ]
    }
  );

  var notEveryDayDb = function (n) {
    return {
      replies: {
	'#test-channel': [
	  {
            count: n,
	    flags: '',
	    name: 'every-day',
	    regex: 'everyday',
	    reply: 'NOT-EVERYDAY',
	  }
	]
      }
    };
  };

  test('auto-reply',
    {
      db: {},
      from: 'test-user', channel: '#test-channel', message: '@reply every-day /everyday/ NOT-EVERYDAY'
    },
    {
      db: notEveryDayDb(10),
      messages: [ { message: 'test-user: OK - auto-reply "every-day" added.', to: '#test-channel' }, ]
    }
  );

  test('auto-reply',
    {
      db: notEveryDayDb(10),
      from: 'test-user', channel: '#test-channel', message: 'everyday'
    },
    {
      db: notEveryDayDb(9),
      messages: [ { message: 'NOT-EVERYDAY', to: '#test-channel' }, ]
    }
  );

  test('auto-reply',
    {
      db: notEveryDayDb(10),
      from: 'test-user', channel: '#test-channel', message: '@reply delete ED'
    },
    {
      db: notEveryDayDb(10),
      messages: [ { message: 'test-user: Sorry - auto-reply "ED" not found.', to: '#test-channel' }, ]
    }
  );

  test('auto-reply',
    {
      db: notEveryDayDb(10),
      from: 'test-user', channel: '#test-channel', message: '@reply delete every-day'
    },
    {
      db: { replies: { '#test-channel': [] } },
      messages: [ { message: 'test-user: OK - auto-reply "every-day" removed.', to: '#test-channel' }, ]
    }
  );

  testIO('http-title', 'http://earldouglas.com/', 'James Earl Douglas');

  testR('stock',
    { db: {}, from: 'test-user', channel: '#test-channel', message: 'Hello @stock GOOG world' },
    { db: {}, messages: [ { message: /^Alphabet Inc. \(GOOG\): \$\d+\.\d+$/,
                            to: '#test-channel' }, ]
    }
  );

  test('tell',
    {
      db: {},
      from: 'test-user', channel: '#test-channel', message: '@tell test-user-2 Welcome back!'
    },
    {
      db: {
        messages: {
          '#test-channel': {
            'test-user-2': [
              {
                date: utilities.now(),
                from: 'test-user',
                message: 'Welcome back!',
                to: 'test-user-2',
              }
            ]
          }
        }
      },
      messages: [ { message: "I'll pass your message along.", to: '#test-channel' }, ]
    }
  );

  test('tell',
    {
      db: {
        messages: {
          '#test-channel': {
            'test-user-2': [
              {
                date: utilities.now(),
                from: 'test-user',
                message: 'Welcome back!',
                to: 'test-user-2',
              }
            ]
          }
        }
      },
      from: 'test-user-2', channel: '#test-channel', message: 'Howdy.'
    },
    {
      db: {
        messages: {
          '#test-channel': {
          }
        }
      },
      messages: [ { message: 'test-user-2: test-user said "Welcome back!" at ' + utilities.now(), to: '#test-channel' }, ]
    }
  );

  test('note',
    {
      db: {},
      from: 'test-user', channel: '#test-channel', message: '@note'
    },
    {
      db: { contactinfo: {} },
      messages: [ { message: 'Usage: @note <message>', to: '#test-channel' }, ]
    }
  );

  test('note',
    {
      db: {},
      from: 'test-user', channel: '#test-channel', message: '@note Testing is hard.'
    },
    {
      db: { contactinfo: {} },
      messages: [ { message: 'test-user: PM me your email address with: /msg sectery-test @setup email name@example.com', to: '#test-channel' }, ]
    }
  );

  var kryptoDb = function (channel, options) {
    var kryptoGame = new krypto.Krypto();
    for (var k in options) {
      if (options.hasOwnProperty(k)) {
        kryptoGame[k] = options[k];
      }
    }
    var db = { krypto: {} };
    db.krypto[channel] = kryptoGame;
    return db;
  };

  testR('krypto',
    {
      db: kryptoDb('#test-channel', { hand: [[8], [13], [14], [15], [12], [1]] }),
      from: 'test-user', channel: '#test-channel', message: '@cards' },
    {
      db: kryptoDb('#test-channel', { hand: [[8], [13], [14], [15], [12], [1]] }),
      messages: [ { message: /^(\d+,?\s+){5}Objective\s+Card:\s+\d+$/, to: '#test-channel' }, ]
    }
  );

  test('krypto',
    {
      db: kryptoDb('#test-channel', { hand: [[8], [13], [14], [15], [12], [1]] }),
      from: 'test-user', channel: '#test-channel', message: '@guess 0'
    },
    {
      db: kryptoDb('#test-channel', { hand: [[8], [13], [14], [15], [12], [1]] }),
      messages: [ { message: 'test-user: please say "@krypto" first!', to: '#test-channel' } ]
    }
  );

  test('krypto',
    {
      db: kryptoDb('#test-channel', { hand: [[8], [13], [14], [15], [12], [1]] }),
      from: 'test-user', channel: '#test-channel', message: '@krypto'
    },
    {
      db: kryptoDb('#test-channel', { guesser: 'test-user', hand: [[8], [13], [14], [15], [12], [1]] }),
      messages: [ { message: 'test-user: OK - take a guess.', to: '#test-channel' } ]
    }
  );

  test('krypto',
    {
      db: kryptoDb('#test-channel', { guesser: 'test-user', hand: [[8], [13], [14], [15], [12], [1]] }),
      from: 'test-user-2', channel: '#test-channel', message: '@guess 0'
    },
    {
      db: kryptoDb('#test-channel', { guesser: 'test-user', hand: [[8], [13], [14], [15], [12], [1]] }),
      messages: [ { message: "test-user-2: sorry, but it's test-user's turn.", to: '#test-channel' } ]
    }
  );

  testIO('eval', '@eval 6 * 7', '42');

  test('points',
    { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } }
          }
    , from: 'test-user', channel: '#test-channel', message: 'foo bar test-user-2++'
    }
  , { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } }
          , points: { '#test-channel': { 'test-user-2': 1 } }
          }
    , messages: [ { message: 'test-user-2 has 1 point', to: '#test-channel' } ]
    }
  );

  test('points',
    { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } }
          , points: { '#test-channel': { 'test-user-2': 1 } }
          }
    , from: 'test-user', channel: '#test-channel', message: 'foo bar test-user-2--'
    }
  , { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } }
          , points: { '#test-channel': { 'test-user-2': 0 } }
          }
    , messages: [ { message: 'test-user-2 has no points', to: '#test-channel' } ]
    }
  );

});
