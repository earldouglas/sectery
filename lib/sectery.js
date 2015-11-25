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
  db.cron = [];
  db.poll['#sectery'] = {id:0, polls:[]};

  var sendReply = teep.fn.throttle(5, 10000, 250, function (reply) {
    if (reply && reply.callback) {
      reply.callback(client.say.bind(client));
    } else if (reply && reply.to && reply.message) {
      client.say(reply.to, reply.message);
    }
  });

  function sendReplies(replies) {
    if (replies) {
      replies.forEach(function (reply) {
        sendReply(reply);
      });
    }
    saveDb(db);
  }

  client.addListener('join', function (channel, nick, message) {
    function run(joinListener) {
      sendReplies(joinListener(db, channel, nick, message));
    }
    loadListeners('join').forEach(run);
  });

  client.addListener('part', function(channel, nick, reason, message) {
    function run(partListener) {
      sendReplies(partListener(db, channel, nick, reason, message));
    }
    loadListeners('part').forEach(run);
  });

  client.addListener('names', function(channel, names) {
    function run(namesListener) {
      sendReplies(namesListener(db, channel, names));
    }
    loadListeners('names').forEach(run);
  });

  var messageCallback = function(from, to, message) {
    function run(messageListener) {
      try {
        sendReplies(messageListener(db, from, to, message,messageCallback));
      } catch (e) {
        console.log('running message listener', e);
      }
    }
    loadListeners('message').forEach(run);
  };

  client.addListener('message', messageCallback); 

  client.addListener('pm', function (from, message) {
    function run(pmListener) {
      sendReplies(pmListener(db, from, message));
    }
    loadListeners('pm').forEach(run);
  });

}

module.exports = sectery;
