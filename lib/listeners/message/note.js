'use strict';

var util = require('../../utilities');

function messageListener(db, from, channel, message) {

  db.contactinfo = db.contactinfo || {};

  var messages = [];

  if (/^@note/.test(message)) {
    var match = /^@note\s+(.+)$/.exec(message);
    if (match) {
      if (db.contactinfo[from] && db.contactinfo[from].email) {
        messages.push({ to: channel, message: 'I\'ll email you a reminder.' });
        var to = db.contactinfo[from].email;
        util.email(db, to, match[1], '');
      } else {
        messages.push({
          to: channel,
          message: from + ': PM me your email address with: /msg ' + util.bot() + ' @setup email name@example.com'
        });
      }
    } else {
      messages.push({ to: channel, message: 'Usage: @note <message>' });
    }
  }

  return messages;
}

module.exports = messageListener;
