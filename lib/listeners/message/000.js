'use strict';

var util = require('../../utilities');

function saveMsg(db, from, channel, message) {
  return (!/^@.*/.test(message) &&
          from != util.bot() &&
          !/\s*s\/(.*)\/(.*)\//.test(message));
}

function messageListener(db, from, channel, message) {
  
  // @grab uses lastsaid
  db.lastsaid = db.lastsaid || {};

  if (saveMsg(db, from, channel, message)) {
    db.lastsaid[from] = message;
  } else {
    db.lastsaid[from] = db.lastsaid[from] || '';
  }

  // regex uses lastsaidArr
  db.lastsaidArr = db.lastsaidArr || [];
  if (db.lastsaidArr.length < 2) {
    db.lastsaidArr = [];
    for (var k in db.lastsaid) {
      if (db.lastsaid.hasOwnProperty(k)) {
        db.lastsaidArr.push({ from: k, message: db.lastsaid[k] });
      }
    }
  }

  if (saveMsg(db, from, channel, message)) {
    db.lastsaidArr = db.lastsaidArr.filter(function (x) {
      x.from !== from;
    });
    db.lastsaidArr.push({ from: from, message: message });
  }

  return [];
}

module.exports = messageListener;
