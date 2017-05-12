'use strict';

var url = require('url');
var curl = require('../../curl');

function messageListener(db, from, channel, message, reply) {
  var match = /(https?:\/\/[^ ]+)/.exec(message)
  if (match) {
    curl(url, function (res) {
      var match = /<title>(.+?)<\/title>/.exec(res);
      if (match) {
        var decoded = match[1].replace(/&#\d+;/gm,function(s) {
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
