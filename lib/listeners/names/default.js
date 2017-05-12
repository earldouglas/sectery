'use strict';

function namesListener(db, channel, names, reply) {
  if (!db.nicks) { db.nicks = {}; }
  if (!db.nicks[channel]) { db.nicks[channel] = {}; }
  Object.keys(names).forEach(function (name) {
    db.nicks[channel][name] = true;
  });
}

module.exports = namesListener;
