'use strict';

// Use instrumented code for code coverage tests
var lib = process.env.LIB_COV ? 'lib-cov' : 'lib';

var sectery = require('../' + lib + '/sectery');
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
sectery(client);

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
  '@names': function(test) {
    test.expect(9);

    client._join('#test-channel','fred',"what up?");
    test.equal(client._lastSaid().message, 'Hey, fred!');

    client._join('#test-channel','testuser',"what up?");
    test.equal(client._lastSaid().message, 'Hey, testuser!');

    client._join('#test-channel','bob',"yo");
    client._join('#test-channel','foo',"doh");
    client._part('#test-channel','bob','i-don\'t-know',"yo");

    client._message('testuser', '#test-channel', '@names');
    test.equal(client._said[client._said.length - 1].message, 'testuser');
    test.equal(client._said[client._said.length - 2].message, 'fred');
    test.equal(client._said[client._said.length - 3].message, 'foo');

    client._join('#test-channel','bob',"yo");

    client._message('testuser', '#test-channel', '@names');
    test.equal(client._said[client._said.length - 1].message, 'testuser');
    test.equal(client._said[client._said.length - 2].message, 'fred');
    test.equal(client._said[client._said.length - 3].message, 'foo');
    test.equal(client._said[client._said.length - 4].message, 'bob');


    // clean up
    client._part('#test-channel','fred','i-don\'t-know',"yo");
    client._part('#test-channel','testuser','i-don\'t-know',"yo");
    client._part('#test-channel','foo','i-don\'t-know',"yo");
    client._part('#test-channel','bob','i-don\'t-know',"yo");

    test.done();
  },
};
