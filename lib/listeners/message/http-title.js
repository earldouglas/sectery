'use strict';

var curl = require('../../curl');

function messageListener(db, from, channel, message, reply) {
  var match = /(https?:\/\/[^ ]+)/.exec(message);
  if (match) {
    curl(match[1], function (res) {
      var elem = /<title>(.+?)<\/title>/.exec(res);
      if (elem) {
        var decoded = elem[1].replace(/&#\d+;/gm,function(s) {
          return String.fromCharCode(s.match(/\d+/gm)[0]);
        });
        decoded = decoded.replace(/&amp;/g, "&")
                         .replace(/&lt;/g, "<")
                         .replace(/&gt;/g, ">")
                         .replace(/&quot;/g, "\"")
                         .replace(/&#39;/g, "'");
        reply({ to: channel, message: decoded });
      }
    });
  }

}

module.exports = messageListener;
