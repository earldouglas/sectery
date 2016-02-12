'use strict';

var util = require('../../utilities');

function messageListener(db, from, channel, message) {
  
  var messages = [];

  var match = /\s*s\/(.*)\/(.*)\//.exec(message);

  if (match) {

    db.lastsaidArr = db.lastsaidArr || [];

    var oldR = new RegExp(match[1]);
    var newS = match[2];

    var entry = db.lastsaidArr.reverse().find(function (x) {
      return oldR.test(x.message);
    });

    if (entry) {
      messages.push({
        to: channel,
        message: '<' + entry.user + '>: ' +
                 entry.message.replace(oldR, newS),
      });
    }

  } 

  return messages;

}

module.exports = messageListener;
