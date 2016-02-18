'use strict';

var httpclient = require('../../http-client');

function messageListener(db, from, channel, message) {

  var match = /(https?:\/\/[^ ]+)/.exec(message)

  if (match) {

    var res = httpclient(db, match[1]);
    var match = /<title>(.+)<\/title>/.exec(res);

    if (match) {
      var decoded = match[1].replace(/&#\d+;/gm,function(s) {
        return String.fromCharCode(s.match(/\d+/gm)[0]);
      });
      decoded = decoded.replace(/&amp;/g, "&")
                       .replace(/&lt;/g, "<")
                       .replace(/&gt;/g, ">")
                       .replace(/&quot;/g, "\"")
                       .replace(/&#39;/g, "'");
      return [ { to: channel, message: decoded } ] ;
    }

  }

}

module.exports = messageListener;
