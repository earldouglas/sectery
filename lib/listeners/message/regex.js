'use strict';

var util = require('../../utilities');

function messageListener(db, from, channel, message) {
  
  var messages = [];

  //save the last message the person said
  var match = /\s*s\/(.*)\/(.*)\//.exec(message);
  if (match) {
    db.lastsaid = db.lastsaid || {};
    db.lastsaid[from] = db.lastsaid[from] || '';

    var oldRegex = new RegExp(match[1]);
    var replace = match[2];
    var newStr = '';
    Object.keys(db.lastsaid).reverse().some(function(user) {
      if (oldRegex.test(db.lastsaid[user])) {
        
        newStr = db.lastsaid[user].replace(oldRegex,replace);
        newStr = '<' + user + '>: '+newStr;
        return true;
      }
      return false;

    });
    messages.push({ to: channel, message: newStr});
  } 
  return messages;
}

module.exports = messageListener;
