'use strict';

function messageListener(db, from, channel, message) {
  if (/^@all/.test(message)) {
    db.nicks = db.nicks || {};
    db.nicks[channel] = db.nicks[channel] || {};
    var namesString = Object.keys(db.nicks[channel]).sort().join(', ');
    return [ { to: channel, message: namesString } ];
  }
}

module.exports = messageListener;
