'use strict';

var util = require('../../utilities');

function messageListener(db, from, channel, message, reply) {

  if (/^@email/.test(message)) {
    var match = /^@email\s+(\w+)\s+(.+)$/.exec(message);
    if (match) {
      var toUser = match[1];
       db.contactinfo = db.contactinfo|| {};

      if (db.contactinfo[toUser] && db.contactinfo[toUser ].email) {
        reply({ to: channel, message: 'I\'ll email ' + toUser  + '.' });
        var to = db.contactinfo[toUser ].email;
        util.email(db, to, '@email from ' + from, match[2]);
      } else {
        reply({
          to: channel,
          message: from + ': Sorry, ' + toUser + ' doesn\'t have their email address setup.'
        });
        reply({
          to: channel,
          message: toUser + ': PM me your email address with: /msg ' + util.bot() + ' @setup email name@example.com'
        });
      }
    } else {
      reply({ to: channel, message: 'Usage: @email <user> <message>' });
    }
  }

}

module.exports = messageListener;

module.exports.help = [{ cmd:'@email',
                         syntax: '@email <user> <message>',
                         output: {success: ['I\'ll email <user>.'],
                                  failure: ['<From user>: Sorry <user> doesn\'t have their email address setup.',
                                            '<To user>: PM me your email address with: /msg <bot> @setup email name@example.com']}
                       }];
