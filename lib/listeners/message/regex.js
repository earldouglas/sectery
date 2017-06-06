'use strict';

var util = require('../../utilities');

function messageListener(db, from, channel, message, reply) {
  
  var match = /\s*s\/(.*)\/(.*)\//.exec(message);

  if (match) {

    db.lastsaidArr = db.lastsaidArr || [];

    var oldR = new RegExp(match[1]);
    var newS = match[2];

    
    for (var i = db.lastsaidArr.length - 1; i >= 0; i--) {
      var entry = db.lastsaidArr[i];
      if (oldR.test(entry.message)) {
        reply({
          to: channel,
          message: '<' + entry.from + '>: ' +
                   entry.message.replace(oldR, newS),
        });
        return;
      }
    }

  } 
}

module.exports = messageListener;
