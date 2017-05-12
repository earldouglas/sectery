'use strict';

function joinListener(db, channel, nick, message, reply) {
  db.nicks = db.nicks || {};
  db.nicks[channel] = db.nicks[channel] || {};
  db.nicks[channel][nick] = true;
  reply({ to: channel, message: 'Hey, ' + nick + '!' });
}

module.exports = joinListener;
