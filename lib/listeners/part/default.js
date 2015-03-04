'use strict';

function partListener(db, channel, nick, reason, message) {
  db.nicks = db.nicks || {};
  db.nicks[channel] = db.nicks[channel] || {};
  db.nicks[channel][nick] = true;
}

module.exports = partListener;
