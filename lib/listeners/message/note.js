'use strict';

var util = require('../../utilities');

function messageListener(db, from, channel, message, reply) {

  db.contactinfo = db.contactinfo || {};

  if (/^@note/.test(message)) {
    var match = /^@note\s+(.+)$/.exec(message);
    if (match) {
      if (db.contactinfo[from] && db.contactinfo[from].email) {
        reply({ to: channel, message: 'I\'ll email you a reminder.' });
        var to = db.contactinfo[from].email;
        util.email(db, to, match[1], '');
      } else {
        reply({
          to: channel,
          message: from + ': PM me your email address with: /msg ' + util.bot() + ' @setup email name@example.com'
        });
      }
    } else {
      reply({ to: channel, message: 'Usage: @note <message>' });
    }
  }

}

module.exports = messageListener;

module.exports.help = [{ cmd:'@note',
                         syntax: '@note <message>',
                         output: {success: ['I\'ll email you a reminder.'],
                                  failure: ['<user>: PM me your email address with: /msg ' + util.bot() + ' @setup email name@example.com']}
                       }];

