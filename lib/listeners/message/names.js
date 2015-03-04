'use strict';

function messageListener(db, from, channel, message) {
  if (/^@names/.test(message)) {
    db.nicks = db.nicks || {};
    db.nicks[channel] = db.nicks[channel] || {};
    var replies = [];
    replies.push({ to: channel, message: 'names:' });
    Object.keys(db.nicks[channel]).sort().forEach(function (nick) {
      replies.push({ to: channel, message: nick });
    });
    return replies;
  }
}

module.exports = messageListener;
