'use strict';

var utilities = require('../../utilities');

function messageListener(db, from, channel, message, reply) {

  db.messages = db.messages || {};
  db.messages[channel] = db.messages[channel] || {};
  db.messages[channel][from] = db.messages[channel][from] || [];

  db.messages[channel][from].forEach(function (msg) {
    reply({
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
      reply({ to: channel, message: 'I\'ll pass your message along.' });
    } else {
      reply({ to: channel, message: 'Usage: @tell <username> <message>'});
    }
  }

}

module.exports = messageListener;

module.exports.help = [{ cmd:'@tell',
                         syntax: '@tell <username> <message>',
                         output: {success: ['I\'ll pass your message along.',
                                            '<user to>: <user from> said "<message>" at <date>'],
                                  failure: []}
                       }];

