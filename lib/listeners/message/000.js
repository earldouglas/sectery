'use strict';

var util = require('../../utilities');

function saveMsg(db, from, channel, message) {
  return (!/^@.*/.test(message) &&
          from != util.bot() &&
          !/\s*s\/(.*)\/(.*)\//.test(message));
}

function messageListener(db, from, channel, message, reply) {
  
  // @grab uses lastsaid
  db.lastsaid = db.lastsaid || {};

  if (saveMsg(db, from, channel, message)) {
    db.lastsaid[from] = message;
  } else {
    db.lastsaid[from] = db.lastsaid[from] || '';
  }

  // regex uses lastsaidArr
  db.lastsaidArr = db.lastsaidArr || [];

  if (saveMsg(db, from, channel, message)) {
    db.lastsaidArr = db.lastsaidArr.filter(function (x) {
      return x.from !== from;
    });
    db.lastsaidArr.push({ from: from, message: message });
  }

}

module.exports = messageListener;
