'use strict';

// Use instrumented code for code coverage tests
var lib = process.env.LIB_COV ? 'lib-cov' : 'lib';

var main = require('../' + lib + '/main');
var nconf = require('nconf');
var configFile = 'config.example.test.json'; // using testing config with API key
var ircUser = 'testbot';

function mockClient() {
  return {
    _channels: {},
    _nicks: {},
    _said: [],
    _lastSaid: function() {
      return this._said[this._said.length - 1];
    },
    join: function (channel) {
      this._channels[channel] = true;
      this.names(channel);
    },
    part: function (channel) {
      delete this._channels[channel];
    },
    names: function(channel) {
      this._nicks[channel] = this._nicks[channel] || [];
      this._names(channel,this._nicks[channel]);
    },
    _pm: function (from, to, message) {
      this._said.push({ from: from, to: to, message: message });
      if (from !== ircUser) {
        this.listeners('pm').forEach(function (listener) {
          listener(from, message);
        });
      }
    },
    _join: function(channel, nick, message) {
      this._nicks[channel] = this._nicks[channel] || {};
      this._nicks[channel][nick] = '';
      this.listeners('join').forEach(function (listener) {
        listener(channel, nick, message);
      });
    },
    _part: function(channel, nick, reason, message) {
      this._nicks[channel] = this._nicks[channel] || {};
      delete this._nicks[channel][nick];
      this.listeners('part').forEach(function (listener) {
        listener(channel, nick, reason, message);
      });
    },
    _names: function(channel,names) {
      this.listeners('names').forEach(function (listener) {
        listener(channel,names);
      });
    },
    _message: function (from, to, message) {
      this._said.push({ from: from, to: to, message: message });
      if (from !== ircUser) {
        this.listeners('message').forEach(function (listener) {
          listener(from, to, message);
        });
      }
    },
    _error: function (error) {
      this.listeners('error').forEach(function (listener) {
        listener(error);
      });
    },
    say: function(to, message) {
      this._message(ircUser, to, message);
    },
    _listeners: {},
    listeners: function(event) {
      return this._listeners[event] || [];
    },
    removeListener: function(event, listener) {
      this._listeners[event] = this._listeners[event] || [];
      for (var i = 0; i < this._listeners[event].length; i++) {
        if (this._listeners[event][i] === listener) {
          delete this._listeners[event][i];
        }
      }
    },
    addListener: function(event, listener) {
      this._listeners[event] = this._listeners[event] || [];
      this._listeners[event].push(listener);
    },
  };
}

var client = mockClient();
main(client, configFile);

function equal(x, y) {
  if (x === y) {
    return true;
  } else {
    throw new Error(x + ' !== ' + y);
  }
}

function keepTry(f) {
  var start = Date.now();
  function tryIt() {
    try {
      f();
    } catch (e) {
      if (Date.now() - start < 20000) {
        setTimeout(tryIt, 150);
      } else {
        throw e;
      }
    }
  }
  tryIt();
}

exports.sectery = {
  '@tell *': function(test) {
    test.expect(12);
    client._join('#test-channel','fred',"what up?");
    client._join('#test-channel','testuser',"what up?");
    client._join('#test-channel','bob',"yo");
    client._join('#test-channel','foo',"doh");
    client._part('#test-channel','bob','i-don\'t-know',"yo");

    var message = 'Hey-Diddly-Ho!';
    client._message('testuser', '#test-channel', '@tell * ' + message);
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'I\'ll pass your message along.');

    client._message('fred', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'fred: testuser said ' + message);

    client._message('testuser', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'Hey, everyone!');

    // bob should not get the message.
    client._join('#test-channel','bob',"yo");
    client._message('bob', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message,  'Hey, everyone!');

    client._message('foo', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'foo: testuser said ' + message);

    //testuser should not get the message
    client._message('testuser', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message,  'Hey, everyone!');
    client._part('#test-channel','fred','i-don\'t-know',"yo");
    client._part('#test-channel','testuser','i-don\'t-know',"yo");
    client._part('#test-channel','foo','i-don\'t-know',"yo");
    client._part('#test-channel','bob','i-don\'t-know',"yo");

    test.done();
  },

  '@join': function(test) {
    test.expect(3);
    test.deepEqual(client._channels, {});
    client._pm('testuser', ircUser, '@join #test-channel-1');
    test.deepEqual(client._channels, { '#test-channel-1': true });
    client._pm('testuser', ircUser, '@join #test-channel-2');
    test.deepEqual(client._channels, { '#test-channel-1': true, '#test-channel-2': true });
    test.done();
  },
  '@part': function(test) {
    test.expect(3);
    test.deepEqual(client._channels, { '#test-channel-1': true, '#test-channel-2': true });
    client._pm('testuser',ircUser, '@part #test-channel-1');
    test.deepEqual(client._channels, { '#test-channel-2': true });
    client._pm('testuser', ircUser, '@part #test-channel-2');
    test.deepEqual(client._channels, {});
    test.done();
  },

  '@tell *: join full channel w/ no joins/parts ': function(test) {
    test.expect(12);
    client._join('#test-channel','fred',"what up?");
    client._join('#test-channel','testuser',"what up?");
    client._join('#test-channel','bob',"yo");
    client._join('#test-channel','foo',"doh");
    client._part('#test-channel','bob','i-don\'t-know',"yo");
    client.part('#test-channel');
    client.join('#test-channel');

    var message = 'Hey-Diddly-Ho!';
    client._message('testuser', '#test-channel', '@tell * ' + message);
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'I\'ll pass your message along.');

    client._message('fred', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'fred: testuser said ' + message);

    client._message('testuser', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'Hey, everyone!');

    // bob should not get the message.
    client._join('#test-channel','bob',"yo");
    client._message('bob', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message,  'Hey, everyone!');

    client._message('foo', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'foo: testuser said ' + message);

    //testuser should not get the message
    client._message('testuser', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message,  'Hey, everyone!');

    test.done();
  },
  '@date': function(test) {
    test.expect(2);

    var nowish = Date.now();
    client._message('testuser', '#test-channel', '@date');

    test.equal(client._lastSaid().to, '#test-channel');

    var date = Date.parse(client._lastSaid().message);
    var difference = Math.abs(nowish - date);
    test.ok(difference < 2000);

    test.done();
  },
  'error': function(test) {
    test.expect(2);

    client._error('oh noes!');

    test.equal(client._said[client._said.length - 1].to, 'testuser');
    test.equal(client._said[client._said.length - 1].message, 'error: oh noes!');

    test.done();
  },
//  '@krypto': function(test) {
//    client._message('kryptobot', '#test-channel', 'Cards: 5, 25, 10, 1, 14 Objective Card: 21');
//
//    // wait for solution to be retrieved
//    keepTry(function() {
//      client._message('testuser', '#test-channel', '@krypto');
//      equal(client._said[client._said.length - 1].to, '#test-channel');
//      equal(client._said[client._said.length - 1].message, '::krypto');
//
//      // wait for auto delay between ::krypto and ::guess messages
//      keepTry(function() {
//        equal(client._said[client._said.length - 1].to, '#test-channel');
//        equal(client._said[client._said.length - 1].message, '::guess (((5 + 1) * 10) - (25 + 14))');
//        test.done();
//      });
//    });
//
//  },
  '@note': function(test) {
    test.expect(4);

    client._message('testuser', '#test-channel', '@note remind me to be reminded');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'I\'ll email you a reminder.');

    client._message('testuser1', '#test-channel', '@note remind me to be reminded');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'I don\'t know your email address.');

    test.done();
  },
  '@scala': function(test) {
    client._message('testuser', '#test-channel', '@scala 2 + 3');

    // wait for response from scala evaluator
    keepTry(function() {

      equal(client._lastSaid().to, '#test-channel');
      equal(true, /res\d+: Int = 5\n\n/.test(client._lastSaid().message));

      client._message('testuser', '#test-channel', '@scala 5 + 7');

      // wait for response from scala evaluator
      keepTry(function() {

        equal(client._lastSaid().to, '#test-channel');
        equal(true, /res\d+: Int = 12\n\n/.test(client._lastSaid().message));

        test.done();
      });

    });
  },
  '@sms': function(test) {
    test.expect(4);

    client._message('testuser', '#test-channel', '@sms read this on your phone');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'I\'ll text you a reminder.');

    client._message('testuser1', '#test-channel', '@sms read this on your phone');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'I don\'t know your phone number.');

    test.done();
  },
  '@tell': function(test) {
    test.expect(4);

    client._message('testuser', '#test-channel', '@tell testuser1 Welcome back!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'I\'ll pass your message along.');

    client._message('testuser1', '#test-channel', 'Hey, everyone!');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, 'testuser1: testuser said Welcome back!');

    test.done();
  },
  '@translate': function(test) {
    client._message('testuser', '#test-channel', '@translate en es Hello, world!');

    // wait for response from translation service
    keepTry(function() {

      equal(client._lastSaid().to, '#test-channel');
      equal(client._lastSaid().message, 'Hola Mundo!');

      test.done();

    });
  },
  '@http': function(test) {
    client._message('testuser', '#test-channel', 'https://www.google.com/');

    // wait for response from http service
    keepTry(function() {

      equal(client._lastSaid().to, '#test-channel');
      equal(client._lastSaid().message, '| Google');

      test.done();

    });
  },
  'yesman': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', "IRC bots sure are handy. Isn't that right, sectery?");
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, "I'll say, testuser.  I'll say.");
    test.done();
  },
  'emoji': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', 'foo bar table flip baz');
    test.equal(client._lastSaid().to, '#test-channel');
    test.equal(client._lastSaid().message, '╯°□°）╯︵ ┻━┻');
    test.done();
  },
  'down': function(test) {
    client._message('testuser', '#test-channel', '@down https://www.google.com/');

    // wait for response from downforjustme service
    keepTry(function() {
      equal(client._lastSaid().to, '#test-channel');
      equal(client._lastSaid().message, "It's just you.  https://www.google.com/ is up.");
      test.done();
    });
  },
  '@weather :: zip code': function(test) {
    client._message('testuser', '#test-channel', '@weather 90210');

    // wait for response from weather service
    keepTry(function() {
      equal(client._lastSaid().to, '#test-channel');
      equal(true, /^Temp/.test(client._lastSaid().message));
      test.done();

    });
  },
  '@weather :: city': function(test) {
    client._message('testuser', '#test-channel', '@weather San Francisco');

    // wait for response from weather service
    keepTry(function() {
      equal(client._lastSaid().to, '#test-channel');
      equal(true, /^Temp/.test(client._lastSaid().message));
      test.done();
    });
  },
  '@weather :: forecast :: city': function(test) {
    client._message('testuser', '#test-channel', '@weather forecast San Francisco');

    // wait for response from weather service
    keepTry(function() {
      equal(client._lastSaid().to, '#test-channel');
      equal(true, /^(Mon|Tues|Wednes|Thurs|Fri|Satur|Sun)day/.test(client._lastSaid().message));
      test.done();
    });
  },
};
