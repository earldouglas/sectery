'use strict';

var fs = require('fs');
var teep = require('teep');

function loadListeners(eventName) {
  var listeners = [];
  fs.readdirSync(__dirname + '/listeners/' + eventName + '/').forEach(function(file) {
    if (/^[^\.].*.js$/.test(file)) {
      var path = './listeners/' + eventName + '/' + file;
      listeners.push(require(path));
    }
  });
  return listeners;
}

var dbFile = process.env.HOME + '/.sectery.json';

function getDb() {
  if (fs.existsSync(dbFile)) {
    return JSON.parse(fs.readFileSync(dbFile));
  } else {
    return {};
  }
}

function saveDb(db) {
  fs.writeFileSync(dbFile, JSON.stringify(db, null, 2));
}

function sectery(client) {

  var db = getDb();

  var sendReply = teep.fn.throttle(5, 10000, 250, function (reply) {
    if (reply && reply.to && reply.message) {
      client.say(reply.to, reply.message);
    }
  });

  client.addListener('join', function (channel, nick, message) {
    function run(joinListener) {
      joinListener(db, channel, nick, message, sendReply);
      saveDb(db);
    }
    loadListeners('join').forEach(run);
  });

  client.addListener('part', function(channel, nick, reason, message) {
    function run(partListener) {
      partListener(db, channel, nick, reason, message, sendReply);
      saveDb(db);
    }
    loadListeners('part').forEach(run);
  });

  client.addListener('names', function(channel, names) {
    function run(namesListener) {
      namesListener(db, channel, names, sendReply);
      saveDb(db);
    }
    loadListeners('names').forEach(run);
  });

  client.addListener('message', function(from, to, message) {
    function run(messageListener) {
      try {
        messageListener(db, from, to, message, sendReply);
      } catch (e) {
        console.error('running message listener', e);
      }
      saveDb(db);
    }
    loadListeners('message').forEach(run);
  });

  client.addListener('pm', function (from, message) {
    function run(pmListener) {
      pmListener(db, from, message, sendReply);
      saveDb(db);
    }
    loadListeners('pm').forEach(run);
  });

}

module.exports = sectery;
