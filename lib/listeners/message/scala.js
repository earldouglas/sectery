'use strict';

var scala = require('../../scala');

function messageListener(db, from, channel, message) {

  db.scala = db.scala || {};
  db.scala.headers = db.scala.headers || {};

  if (/^@scala/.test(message)) {
    var match = /^@scala\s+(.+)$/.exec(message);
    if (match) {
      var res = scala(db, match[1]);
      return [ { to: channel, message: res } ] ;
    } else {
      return [ { to: channel, message: 'Usage: @scala <expression>' } ];
    }
  }

}

module.exports = messageListener;
