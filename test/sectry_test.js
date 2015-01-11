'use strict';

// Use instrumented code for code coverage tests
var lib = process.env.LIB_COV ? 'lib-cov' : 'lib';

var fsdb = require('../' + lib + '/fsdb');
var main = require('../' + lib + '/main');

var configFile = 'config.json.example';
var config = fsdb.load(configFile);

function mockClient() {
  return {
    _channels: {},
    _said: [],
    _lastSaid: function() {
      return this._said[this._said.length - 1];
    },
    join: function (channel) {
      this._channels[channel] = true;
    },
    part: function (channel) {
      delete this._channels[channel];
    },
    _pm: function (from, to, message) {
      this._said.push({ from: from, to: to, message: message });
      if (from !== config.irc.user) {
        this.listeners('pm').forEach(function (listener) {
          listener(from, message);
        });
      }
    },
    _message: function (from, to, message) {
      this._said.push({ from: from, to: to, message: message });
      if (from !== config.irc.user) {
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
      this._message(config.irc.user, to, message);
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

exports['sectery'] = {
  '@join': function(test) {
    test.expect(3);
    test.deepEqual(client._channels, {});
    client._pm('testuser', config.irc.user, '@join #test-channel-1');
    test.deepEqual(client._channels, { '#test-channel-1': true });
    client._pm('testuser', config.irc.user, '@join #test-channel-2');
    test.deepEqual(client._channels, { '#test-channel-1': true, '#test-channel-2': true });
    test.done();
  },
  '@part': function(test) {
    test.expect(3);
    test.deepEqual(client._channels, { '#test-channel-1': true, '#test-channel-2': true });
    client._pm('testuser', config.irc.user, '@part #test-channel-1');
    test.deepEqual(client._channels, { '#test-channel-2': true });
    client._pm('testuser', config.irc.user, '@part #test-channel-2');
    test.deepEqual(client._channels, {});
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
  '@krypto': function(test) {
    test.expect(4);

    client._message('kryptobot', '#test-channel', 'Cards: 5, 25, 10, 1, 14 Objective Card: 21');

    // wait for solution to be retrieved
    setTimeout(function() {
      client._message('testuser', '#test-channel', '@krypto');
      test.equal(client._said[client._said.length - 1].to, '#test-channel');
      test.equal(client._said[client._said.length - 1].message, '::krypto');

      // wait for auto delay between ::krypto and ::guess messages
      setTimeout(function() {
        test.equal(client._said[client._said.length - 1].to, '#test-channel');
        test.equal(client._said[client._said.length - 1].message, '::guess (((5 + 1) * 10) - (25 + 14))');
        test.done();
      }, 2000);
    }, 2000);

  },
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
    test.expect(4);

    client._message('testuser', '#test-channel', '@scala 2 + 3');

    // wait for response from scala evaluator
    setTimeout(function() {

      test.equal(client._lastSaid().to, '#test-channel');
      test.ok(/res\d+: Int = 5\n\n/.test(client._lastSaid().message));

      client._message('testuser', '#test-channel', '@scala 5 + 7');

      // wait for response from scala evaluator
      setTimeout(function() {

        test.equal(client._lastSaid().to, '#test-channel');
        test.ok(/res\d+: Int = 12\n\n/.test(client._lastSaid().message));

        test.done();
      }, 2000);

    }, 2000);
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
    test.equal(client._lastSaid().message, 'testuser1: testuser left you a message: Welcome back!');

    test.done();
  },
  '@translate': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', '@translate en es Hello, world!');

    // wait for response from translation service
    setTimeout(function() {

      test.equal(client._lastSaid().to, '#test-channel');
      test.equal(client._lastSaid().message, 'Hola Mundo!');

      test.done();

    }, 1000);
  },
  '@weather': function(test) {
    test.expect(2);

    client._message('testuser', '#test-channel', '@weather san francisco');

    // wait for response from weather service
    setTimeout(function() {

      test.equal(client._lastSaid().to, '#test-channel');
      test.ok(/^Temp/.test(client._lastSaid().message));

      test.done();

    }, 1000);
  },
};
