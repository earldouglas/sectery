'use strict';

var request = require('sync-request');

function messageListener(db, from, channel, message) {

  db.scala = db.scala || {};
  db.scala.headers = db.scala.headers || {};

  if (/^https?:\/\/[^ ]+$/.test(message)) {
    var res = request('GET', message);
    var match = /<title>(.+)<\/title>/.exec(res.getBody().toString().replace(/\n/g, ''));
    if (match) {
      var decoded = match[1].replace(/&#\d+;/gm,function(s) {
        return String.fromCharCode(s.match(/\d+/gm)[0]);
      });
      return [ { to: channel, message: decoded } ] ;
    }
  }
}

module.exports = messageListener;
