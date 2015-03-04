'use strict';

function joinListener(db, channel, nick, message) {
  if (!db.nicks) { db.nicks = {}; }
  if (!db.nicks[channel]) { db.nicks[channel] = {}; }
  db.nicks[channel][nick] = true;
  return [ { to: channel, message: 'Hey, ' + nick + '!' } ];
}

module.exports = joinListener;
