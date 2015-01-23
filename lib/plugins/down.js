'use strict';

var preq = require('preq');

function listener(client) {
  return function(from, to, message) {
    if (/^@down/.test(message)) {
      var match = /^@down\s+(.+)$/.exec(message);
      if (match) {
        preq.get({
          uri: 'http://www.downforeveryoneorjustme.com/' + encodeURIComponent(match[1])
        }).then(function (res) {
          if (/It's not just you/.test(res.body)) {
            client.say(to, "It's not just you!  " + match[1] + ' looks down from here.');
          } else {
            client.say(to, "It's just you.  " + match[1] + ' is up.');
          }
        });
      } else {
        client.say(to, 'Usage: @down <uri>');
      }
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
