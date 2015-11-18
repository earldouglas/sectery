'use strict';

var util = require('../../utilities');

// random between min and max inclusive
function getRandomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

function messageListener(db, from, channel, message) {
  
  var messages = [];

  //save the last message the person said
  if (/^@quote/.test(message)) {
    var match = /^@quote\s+([.-_|\w]+)$/.exec(message);
    db.quotes = db.quotes || {};

    if (match) {
      var sayer = match[1];
      db.quotes[sayer] = db.quotes[sayer] || [];
      if (db.quotes[sayer] && db.quotes[sayer].length !== 0) {
        var rand = getRandomInt(0,db.quotes[sayer].length - 1);
        messages.push({ to: channel, message: '<' + sayer + '>: ' + db.quotes[sayer][rand]});
      } else {
        messages.push({ to: channel, message: from + ': Sorry, ' + util.bot() + ' has not recorded anything for ' + sayer +'.'});
      }
    } else {
      messages.push({ to: channel, message: 'Usage: @quote <username>' });
    }
  }
  return messages;
}

module.exports = messageListener;
