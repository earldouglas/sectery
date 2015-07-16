'use strict';

var util = require('../../utilities');

function messageListener(db, from, channel, message) {
  
  var messages = [];
  //save the last message the person said
  db.lastsaid = db.lastsaid || {};
  db.lastsaid[from] = db.lastsaid[from] || '';
  if (!/^@.*/.test(message) && from != util.bot) {
    db.lastsaid[from] = message;
  }
  if (/^@grab/.test(message)) {
    var match = /^@grab\s+(\w+)$/.exec(message);
    db.quotes = db.quotes || {};

    if (match) {
      var sayer = match[1];
      db.quotes[sayer] = db.quotes[sayer] || [];
      if (db.lastsaid[sayer] && db.lastsaid[sayer] !== '') {
        db.quotes[sayer].push(db.lastsaid[sayer]);
        messages.push({ to: channel, message: from + ': OK - message grabbed.' });
      } else {
        messages.push({ to: channel, message: from + ': Sorry, ' + util.bot + ' has not recorded anything for ' + sayer +'.'});
      }
    } else {
      messages.push({ to: channel, message: 'Usage: @grab <username>' });
    }
  }
  return messages;
}

module.exports = messageListener;
