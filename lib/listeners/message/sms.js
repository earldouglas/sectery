'use strict';

var util = require('../../utilities');

function messageListener(db, from, channel, message) {

  var messages = [];

  if (/^@sms/.test(message)) {
    var match = /^@sms\s+(\w+)\s+(.+)$/.exec(message);
    if (match) {
      var toUser = match[1];
       db.contactinfo = db.contactinfo|| {};

      if (db.contactinfo[toUser] && db.contactinfo[toUser ].sms) {
        messages.push({ to: channel, message: 'I\'ll sms ' + toUser  + '.' });
        var to = db.contactinfo[toUser ].sms;
        util.sms(db,to,match[2]);
      } else {
        messages.push({
          to: channel,
          message: from + ': Sorry, ' + toUser + ' doesn\'t have their phone number setup.'
        });
        messages.push({
          to: channel,
          message: toUser + ': PM me your contact info with: /msg ' + util.bot() + ' @setup sms <phone number>'
        });
      }
    } else {
      messages.push({ to: channel, message: 'Usage: @sms <user> <message>' });
    }
  }

  return messages;
}

module.exports = messageListener;

module.exports.help = [{ cmd:'@sms',
                         syntax: '@sms <username> <message>',
                         output: {success: ['I\'ll sms <username>.'],
                                  failure: ['<from user>: Sorry, <username> doesn\'t have their phone number setup',
                                            '<username>: PM me your contact info with: /msg ' + util.bot() + ' @setup sms <phone number>']}
                       }];
