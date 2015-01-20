'use strict';

var preq = require('preq');

function listener(client) {
  return function(from, to, message) {
    if (/^https?:\/\/[^ ]+$/.test(message)) {
      preq.get({ uri: message })
      .then(function (res) {
        var match = /<title>(.+)<\/title>/.exec(res.body);
        if (match) {
          var decoded = match[1].replace(/&#\d+;/gm,function(s) {
            return String.fromCharCode(s.match(/\d+/gm)[0]);
          });
          client.say(to, '| ' + decoded);
        } else {
          client.say(to, 'I can\'t find a title for that page.');
        }
      });
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
