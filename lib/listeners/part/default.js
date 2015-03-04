'use strict';

function partListener(db, channel, nick, reason, message) {
  if (!db.nicks) { db.nicks = {}; }
  if (!db.nicks[channel]) { db.nicks[channel] = {}; }
  db.nicks[channel][nick] = true;
}

module.exports = partListener;
