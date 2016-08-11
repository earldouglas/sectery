'use strict';

function messageListener(db, from, channel, message) {
  var messages = [];
  var match = /^@\s+(.+)$/.exec(message);
  if (match) {
    var result = eval(match[1]);
    messages.push({ to: channel, message: result });
  }
  return messages;
}

module.exports = messageListener;
