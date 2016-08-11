'use strict';

var runsync = require("runsync");

function messageListener(db, from, channel, message) {
  var messages = [];
  var match = /^@\s+(.+)$/.exec(message);
  if (match) {
    var result = runsync.popen('haskeval', { input: match[1], encoding: 'utf8' });
    var lines = result.stdout.toString().split('\n');
    for (var i = 0; i < lines.length; i++) {
      var line = lines[i].trim();
      if (line.length > 0) {
        messages.push({ to: channel, message: line });
      }
    }
  }
  return messages;
}

module.exports = messageListener;
