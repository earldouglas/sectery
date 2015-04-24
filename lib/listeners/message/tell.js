'use strict';

function messageListener(db, from, channel, message) {

  var messages = [];

  db.messages = db.messages || {};
  db.messages[channel] = db.messages[channel] || {};
  db.messages[channel][from] = db.messages[channel][from] || [];

  db.messages[channel][from].forEach(function (msg) {
    messages.push({
      to: channel,
      message: msg.to + ': ' + msg.from + ' said \"' + msg.message + '\" on ' + msg.date,
    });
  });

  delete db.messages[channel][from];

  if (/^@tell/.test(message)) {
    var match = /^@tell\s+(\w+|\*)\s+(.+)$/.exec(message);
    if (match) {
      var who = match[1];
      db.messages[channel][who] = db.messages[channel][who] || [];
      db.messages[channel][who].push({
        to: who,
        from: from,
        message: match[2],
        date: (new Date()).toUTCString(),
      });
      messages.push({ to: channel, message: 'I\'ll pass your message along.' });
    } else {
      messages.push({ to: channel, message: 'Usage: @tell <username> <message>'});
    }
  }

  return messages;
}

module.exports = messageListener;
