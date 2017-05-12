'use strict';

function partListener(db, channel, nick, reason, message, reply) {
  db.nicks = db.nicks || {};
  db.nicks[channel] = db.nicks[channel] || {};
  delete db.nicks[channel][nick];
}

module.exports = partListener;
