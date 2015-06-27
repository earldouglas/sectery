'use strict';

var utilities = require('../../utilities');

var command = '@tell';

function messageListener(db, from, channel, message) {

  var messages = [];

  db.messages = db.messages || {};
  db.messages[channel] = db.messages[channel] || {};
  db.messages[channel][from] = db.messages[channel][from] || [];

  db.messages[channel][from].forEach(function (msg) {
    messages.push({
      to: channel,
      message: msg.to + ': ' + msg.from + ' said \"' + msg.message + '\" at ' + msg.date,
    });
  });

  delete db.messages[channel][from];

  if (/^@tell/.test(message)) {
    var match = /^@tell\s+([^\s]+)\s+(.+)$/.exec(message);
    if (match) {
      var who = match[1];
      db.messages[channel][who] = db.messages[channel][who] || [];
      db.messages[channel][who].push({
        to: who,
        from: from,
        message: match[2],
        date: utilities.now(),
      });
      messages.push({ to: channel, message: 'I\'ll pass your message along.' });
    } else {
      messages.push({ to: channel, message: 'Usage: @tell <username> <message>'});
    }
  }

  return messages;
}

module.exports = messageListener;
module.exports.command = command;
module.exports.help = command + ' <username> <message>';
module.exports.description = 'This command will save your message to the
module.exports.output = 'I\'ll pass your message along.'

