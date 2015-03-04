'use strict';

function mockClient(ircUser) {
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

module.exports = mockClient;
