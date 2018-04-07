'use strict';

var util = require('../../utilities');

function saveLastMessage(db, from, channel, message) {

  var isSavable =
    !/^\s*$/.test(message) &&
    !/^@.*/.test(message) &&
    from != util.bot() &&
    !/\s*s\/(.*)\/(.*)\//.test(message);

  if (isSavable) {
    db.lastsaid = db.lastsaid || {};
    db.lastsaid[channel] = db.lastsaid[channel] || {};
    db.lastsaid[channel][from] = message;
  }

}

function grabLastMessage(db, from, channel) {

  db.lastsaid = db.lastsaid || {};
  db.lastsaid[channel] = db.lastsaid[channel] || {};
  var message = db.lastsaid[channel][from];
  if (message) {
    db.quotes = db.quotes || {};
    db.quotes[channel] = db.quotes[channel] || {};
    db.quotes[channel][from] = db.quotes[channel][from] || [];
    db.quotes[channel][from].push(message + ' at ' + util.now());
  }
  return message;

}

function messageListener(db, from, channel, message, reply) {
  
  saveLastMessage(db, from, channel, message);

  if (/^@grab/.test(message)) {
    var match = /^@grab\s+([.-_|\w]+)\s*$/.exec(message);
    if (match) {
      var sayer = match[1];
      if (sayer != from) {
        if (grabLastMessage(db, sayer, channel)) {
          reply({ to: channel,
                  message: from + ': OK - message grabbed.'
                });
        } else {
          reply({ to: channel,
                  message: from + ': Sorry, ' + util.bot() + ' has not recorded anything for ' + sayer +'.'
                });
        }
      } else {
        reply({ to: channel,
                message: from + ': Sorry, you can\'t grab yourself.'
             });
      }
    } else {
      reply({ to: channel,
              message: 'Usage: @grab <username>'
            });
    }
  }
}

module.exports = messageListener;
module.exports.help = [{ cmd:'@grab',
                         syntax: '@grab <username>',
                         output: { success: ['<user>: OK - message grabbed.'],
                                   failure: ['<user>: Sorry, <botname> has not recored anything for <username>.',
                                             '<user>: Sorry, you can\'t grab yourself.']
                                 }
                       }];
