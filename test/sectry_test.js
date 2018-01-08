'use strict';

var sectery   = require('../lib/sectery');
var utilities = require('../lib/utilities');
var krypto    = require('../lib/krypto-game');
var assert    = require('assert');

process.env.IRC_USER = 'sectery-test';

describe('pm listeners', function () {

  var test = function (name, req, res) {
    it(name, function (done) {
      var listener = require('../lib/listeners/pm/' + name + '.js');
      var replies = [];
      var reply = function (x) { replies.push(x); };
      listener(req.db, req.from, req.message, reply);
      this.timeout(3000);
      setTimeout(function () {
        assert.deepEqual(replies, res.messages);
        assert.deepEqual(req.db, res.db);
        done();
      }, 2000);
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
      db: {}, messages: [ { message: 'Usage: @setup <email|sms|tz> <email@example.com|phone|code|olson-timezone>', to: 'test-user' } ]
    }
  );

  test('setup',
    { db: {}, channel: '#test-channel', from: 'test-user', message: '@setup tz MST' },
    {
      db: {} , 
      messages: [ { message: 'test-user: please provide a valid Olson timezone. e.g. America/Phoenix', to: 'test-user' } ]
    }
  );

  test('setup',
    { db: {}, channel: '#test-channel', from: 'test-user', message: '@setup tz America/Phoenix' },
    {
      db: { "settings": { "test-user": { "tz": "America/Phoenix" } } } , 
      messages: [ { message: 'test-user: timezone updated.', to: 'test-user' } ]
    }
  );


});

describe('join listeners', function () {

  var test = function (name, req, res) {
    it(name, function (done) {
      var listener = require('../lib/listeners/join/' + name + '.js');
      var replies = [];
      var reply = function (x) { replies.push(x); };
      listener(req.db, req.channel, req.nick, req.message, reply);
      this.timeout(3000);
      setTimeout(function () {
        assert.deepEqual(replies, res.messages);
        assert.deepEqual(req.db, res.db);
        done();
      }, 2000);
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
    it(name, function (done) {
      var listener = require('../lib/listeners/part/' + name + '.js');
      var replies = [];
      var reply = function (x) { replies.push(x); };
      listener(req.db, req.channel, req.nick, req.reason, req.message, reply);
      this.timeout(3000);
      setTimeout(function () {
        assert.deepEqual(replies, res.messages);
        assert.deepEqual(req.db, res.db);
        done();
      }, 2000);
    });
  };

  test('default',
    {
      db: { nicks: { '#test-channel': { 'test-user': true } } },
      channel: '#test-channel', nick: 'test-user', reason: '', message: ''
    },
    {
      db: { nicks: { '#test-channel': {} } },
      messages: []
    }
  );

});

describe('message listeners', function () {

  var test = function (name, req, res) {
    it(name, function (done) {
      var listener = require('../lib/listeners/message/' + name + '.js');
      var replies = [];
      var reply = function (x) { replies.push(x); };
      listener(req.db, req.from, req.channel, req.message, reply);
      this.timeout(3000);
      setTimeout(function () {
        assert.deepEqual(replies, res.messages);
        assert.deepEqual(req.db, res.db);
        done();
      }, 2000);
    });
  };

  var testL = function (name, reqL, resL) {
    it(name, function (done) {
      var req = reqL();
      var res = resL();
      var listener = require('../lib/listeners/message/' + name + '.js');
      var replies = [];
      var reply = function (x) { replies.push(x); };
      listener(req.db, req.from, req.channel, req.message, reply);
      this.timeout(3000);
      setTimeout(function () {
        assert.deepEqual(replies, res.messages);
        assert.deepEqual(req.db, res.db);
        done();
      }, 2000);
    });
  };

  var testR = function (name, req, res) {
    it(name, function (done) {
      var listener = require('../lib/listeners/message/' + name + '.js');
      var replies = [];
      var reply = function (x) { replies.push(x); };
      listener(req.db, req.user, req.channel, req.message, reply);
      this.timeout(3000);
      setTimeout(function () {
        assert.equal(replies.length, res.messages.length);
        for (var i = 0; i < res.messages.length; i++) {
          assert.equal(replies[i].to, res.messages[i].to);
          assert.equal(res.messages[i].message.test(replies[i].message), true);
        }
        assert.deepEqual(req.db, res.db);
        done();
      }, 2000);
    });
  };

  var testIO = function (name, input, outputs) {
    var messages = [];
    for (var i = 0; i < outputs.length; i++) {
      messages.push({ message: outputs[i], to: '#test-channel' });
    }
    test(name,
      { db: {}, from: 'test-user', channel: '#test-channel', message: input },
      { db: {}, messages: messages }
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

  testIO('weather', '@weather', [ '@weather <location>' ]);

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

  testIO('http-title', 'https://earldouglas.com/', [ 'James Earl Douglas' ]);

  testIO('http-title', 'https://frinkiac.com/meme/S10E17/991272/m/Q09NRSBPTiBPVVQsIEJPWSEKSVQnUyBXSU5EWSE=', [ "COME ON OUT, BOY! IT'S WINDY!" ]);

  testL('tell',
    function () {
      return {
               db: {},
               from: 'test-user', channel: '#test-channel', message: '@tell test-user-2 Welcome back!'
             };
    },
    function () {
      return {
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
             };
    }
  );

  testL('tell',
    function () {
      return {
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
             };
    },
    function () {
      return {
               db: {
                 messages: {
                   '#test-channel': {
                   }
                 }
               },
               messages: [ { message: 'test-user-2: test-user said "Welcome back!" at ' + utilities.now(), to: '#test-channel' }, ]
             };
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

  testIO('eval', '@eval 6 * 7', [ '42' ]);

  test('points',
    { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } }
          },
      from: 'test-user', channel: '#test-channel', message: 'foo bar test-user-2++'
    },
    { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } },
            points: { '#test-channel': { 'test-user-2': 1 } }
          },
      messages: [ { message: 'test-user-2 has 1 point.', to: '#test-channel' } ]
    }
  );

  test('points',
    { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } },
            points: { '#test-channel': { 'test-user-2': 1 } }
          },
      from: 'test-user', channel: '#test-channel', message: 'foo bar test-user-2--'
    },
    { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } },
            points: { '#test-channel': { 'test-user-2': 0 } }
          },
      messages: [ { message: 'test-user-2 has no points.', to: '#test-channel' } ]
    }
  );

  test('points',
    { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } },
            points: { '#test-channel': { 'test-user': 1 } }
          },
      from: 'test-user', channel: '#test-channel', message: 'foo bar test-user++'
    },
    { db: { nicks: { '#test-channel': { 'test-user': true, 'test-user-2': true } },
            points: { '#test-channel': { 'test-user': 1 } }
          },
      messages: [ { to: '#test-channel',
                    message: 'test-user: You can\'t change your own points.'
                  } ]
    }
  );

  testR('bands',
    { db: {}, from: 'test-user', channel: '#test-channel', message: '@bands' },
    { db: {},
      messages: [
        { message: /^Bands as of .*/, to: '#test-channel' },
        { message: /^| 12m-10m |.*/, to: '#test-channel' },
        { message: /^| 17m-15m |.*/, to: '#test-channel' },
        { message: /^| 30m-20m |.*/, to: '#test-channel' },
        { message: /^| 80m-40m |.*/, to: '#test-channel' }
      ]
    }
  );

  testR('btc',
    { db: {}, from: 'test-user', channel: '#test-channel', message: '@btc' },
    { db: {},
      messages: [
        { message: /^Bands as of .*/, to: '#test-channel' }
      ]
    }
  );

  test('spacex',
    { db: {}, from: 'test-user', channel: '#test-channel', message: '@spacex' },
    { db: {},
      messages: [
        { message: '@spacex <next>', to: '#test-channel' },
      ]
    }
  );

  testR('spacex',
    { db: { 'settings': { 'test-user': { 'tz': 'America/Denver' } } },
      from: 'test-user', channel: '#test-channel', message: '@spacex next' },
    { db: { 'settings': { 'test-user': { 'tz': 'America/Denver' } } },
      messages: [
        { message: /^Flight \d+ scheduled for \d{2}-\d{2}-\d{4} \d{2}:\d{2}:\d{2} [A-Z]{3}\./, to: '#test-channel' },
        { message: /^(Previously flown|New) (.*) carrying (.*) to (.*) launching from (.*)\.$/, to: '#test-channel' },
        { message: /^(Attempting landing on (.*)|Expendable)/, to: '#test-channel' },
      ]
    }
  );

});

describe('message listeners with time', function () {

  
  var tk        = require('timekeeper');
  var moment    = require('moment-timezone'); 
  var time      = require('../lib/time');

  var test = function (name, req, res, date) {
    it(name, function (done) {

      tk.freeze(date);
      var listener = require('../lib/listeners/message/' + name + '.js');
      var replies = [];
      var reply = function (x) { replies.push(x); };
      listener(req.db, req.from, req.channel, req.message, reply);
      this.timeout(3000);
      setTimeout(function () {
        assert.deepEqual(replies, res.messages);
        assert.deepEqual(req.db, res.db);
        tk.reset();
        done();
      }, 2000);
    });
  };

  var m = moment.parseZone("2017-03-06T17:01:00.000-07:00");
  tk.freeze(m.toDate());
  var afterHours = 'test-user: 5:01 PM MST on Mon, Mar 6th, why are you still here? Go home.';
  test('time',
    { db: {}, from: 'test-user', channel: '#test-channel', message: '@time' },
    { db: {}, messages: [ { message: afterHours,
                            to: '#test-channel' }, ]

    },
    m.toDate()
  );
  tk.reset();

  m = moment.parseZone("2017-03-06T16:01:00.000-07:00");
  var then = m.clone().add(1, 'hour').subtract(1,'minute');
  tk.freeze(m.toDate());

  var delta = time.time_delta(m.toDate(),then);
  var workHours  = 'test-user: 4:01 PM MST on Mon, Mar 6th, ' + delta + ' until you get to go home. Hang in there.';
  test('time',
    { db: {}, from: 'test-user', channel: '#test-channel', message: '@time' },
    { db: {}, messages: [ { message: workHours,
                            to: '#test-channel' }, ]

    },
    m.toDate()
  );
  tk.reset();

  m = moment.parseZone("2017-03-11T16:01:00.000-07:00");
  tk.freeze(m.toDate());
  var weekend    = 'test-user: 4:01 PM MST on Sat, Mar 11th, enjoy your day of not-work.';
  test('time',
    { db: {}, from: 'test-user', channel: '#test-channel', message: '@time' },
    { db: {}, messages: [ { message: weekend,
                            to: '#test-channel' }, ]

    },
    m.toDate()
  );
  tk.reset();

  // 4 pm on the server (America/Phoenix) but the user is in Denver (hour later), expect it an hour later

  m = moment.parseZone("2017-03-13T16:01:00.000-07:00");
  
  var d = new Date(m.tz('America/Phoenix').format());
  tk.freeze(d);
  afterHours = 'test-user: 5:01 PM MDT on Mon, Mar 13th, why are you still here? Go home.';
  test('time',
    { db: { 'settings': { 'test-user': { 'tz': 'America/Denver' } } } , 
      from: 'test-user',
      channel: '#test-channel', message: '@time' },

    { db: { 'settings': { 'test-user': { 'tz': 'America/Denver' } } } , 
      messages: [ { message: afterHours,
                            to: '#test-channel' }, ]

    },
    d
  );
  tk.reset();
});
