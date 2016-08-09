'use strict';

var util = require('../../utilities');

function saveMsg(db,from,channel,message) {
  return (!/^@.*/.test(message) &&
          from != util.bot() &&
          !/\s*s\/(.*)\/(.*)\//.test(message));
}
function messageListener(db, from, channel, message) {
  
  var messages = [];
  //save the last message the person said
  db.lastsaid = db.lastsaid || {};
  db.lastsaid[from] = db.lastsaid[from] || '';
  if (saveMsg(db,from,channel,message)) {
    db.lastsaid[from] = message;
  }
  if (/^@grab/.test(message)) {
    var match = /^@grab\s+([.-_|\w]+)\s*$/.exec(message);
    db.quotes = db.quotes || {};

    if (match) {
      var sayer = match[1];
      if (sayer != from) {
        db.quotes[sayer] = db.quotes[sayer] || [];
        if (db.lastsaid[sayer] && db.lastsaid[sayer] !== '') {
          db.quotes[sayer].push(db.lastsaid[sayer] + ' at ' + util.now());
          messages.push({ to: channel, message: from + ': OK - message grabbed.' });
        } else {
          messages.push({ to: channel, message: from + ': Sorry, ' + util.bot() + ' has not recorded anything for ' + sayer +'.'});
        }
      } else {
        messages.push({ to: channel, message: from + ': Sorry, you can\'t grab yourself.'});
      }
    } else {
      messages.push({ to: channel, message: 'Usage: @grab <username>' });
    }
  }
  return messages;
}

module.exports = messageListener;
module.exports.help = [{ cmd:'@grab',
                         syntax: '@grab <username>',
                         output: {success: ['<user>: OK - message grabbed.'],
                                  failure: ['<user>: Sorry, <botname> has not recored anything for <username>.',
                                            '<user>: Sorry, you can\'t grab yourself.']}
                       }];

