'use strict';

var fs = require('fs');

function loadListeners(eventName) {
  var listeners = [];
  fs.readdirSync(__dirname + '/listeners/' + eventName + '/').forEach(function(file) {
    var path = './listeners/' + eventName + '/' + file;
    listeners.push(require(path));
  });
  return listeners;
}

function sectery(client) {

  var db = {};

  function reply(replies) {
    if (replies) {
      replies.forEach(function (reply) {
        if (reply.to && reply.message) {
          client.say(reply.to, reply.message);
        }
      });
    }
  }

  client.addListener('join', function (channel, nick, message) {
    function run(joinListener) {
      reply(joinListener(db, channel, nick, message));
    }
    loadListeners('join').forEach(run);
  });

  client.addListener('part', function(channel, nick, reason, message) {
    function run(partListener) {
      reply(partListener(db, channel, nick, reason, message));
    }
    loadListeners('part').forEach(run);
  });

  client.addListener('names', function(channel, names) {
    function run(namesListener) {
      reply(namesListener(db, channel, names));
    }
    loadListeners('names').forEach(run);
  });
 
  var messageListeners = loadListeners('message');
  client.addListener('message', function(from, to, message) {
    function run(messageListener) {
      try {
        reply(messageListener(db, from, to, message));
      } catch (e) {
        console.log('running message listener', e);
      }
    }
    messageListeners.forEach(run);
  });

}

module.exports = sectery;
