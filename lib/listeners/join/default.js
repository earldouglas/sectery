'use strict';

function joinListener(db, channel, nick, message) {
  db.nicks = db.nicks || {};
  db.nicks[channel] = db.nicks[channel] || {};
  db.nicks[channel][nick] = true;
  return [ { to: channel, message: 'Hey, ' + nick + '!' } ];
}

module.exports = joinListener;
