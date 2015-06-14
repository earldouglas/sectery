'use strict';

var httpclient = require('../../http-client');

function messageListener(db, from, channel, message) {
  if (/^https?:\/\/[^ ]+$/.test(message)) {
    var res = httpclient(db, message);
    var match = /<title>(.+)<\/title>/.exec(res);
    if (match) {
      var decoded = match[1].replace(/&#\d+;/gm,function(s) {
        return String.fromCharCode(s.match(/\d+/gm)[0]);
      });
      return [ { to: channel, message: decoded } ] ;
    }
  }
}

module.exports = messageListener;
