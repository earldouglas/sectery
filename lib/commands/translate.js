'use strict';

var preq = require('preq');

function listener(client) {
  return function(from, to, message) {
    var match = /^(\w+)\s+(\w\w)\s+(.+)$/.exec(message);
    if (match) {
      var sl = encodeURIComponent(match[1]);
      var tl = encodeURIComponent(match[2]);
      var text = encodeURIComponent(match[3]);
      var url = 'https://translate.google.com?sl=' + sl +
                '&tl=' + tl +
                '&text=' + text;
      preq.get({ uri: url })
      .then(function(res) {
        var match2 = /TRANSLATED_TEXT='([^']*)'/.exec(res.body);
        if (match2) {
          client.say(to, match2[1]);
        }
      });
    } else {
      client.say(to, 'Usage: @translate <src lang> <dest lang> <text>');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
