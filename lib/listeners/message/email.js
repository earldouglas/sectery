'use strict';

var util = require('../../utilities');

function messageListener(db, from, channel, message) {

  var messages = [];

  if (/^@email/.test(message)) {
    var match = /^@email\s+(\w+)\s+(.+)$/.exec(message);
    if (match) {
      var toUser = match[1];
       db.contactinfo = db.contactinfo|| {};

      if (db.contactinfo[toUser] && db.contactinfo[toUser ].email) {
        messages.push({ to: channel, message: 'I\'ll email ' + toUser  + '.' });
        var to = db.contactinfo[toUser ].email;
        util.email(db, to, '@email from ' + from, match[2]);
      } else {
        messages.push({
          to: channel,
          message: from + ': Sorry, ' + toUser + ' doesn\'t have their email address setup.'
        });
        messages.push({
          to: channel,
          message: toUser + ': PM me your email address with: /msg ' + util.bot + ' @setup email name@example.com'
        });
      }
    } else {
      messages.push({ to: channel, message: 'Usage: @email <user> <message>' });
    }
  }

  return messages;
}

module.exports = messageListener;
