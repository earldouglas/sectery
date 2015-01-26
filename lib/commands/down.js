'use strict';

var preq = require('preq');

function listener(client) {
  return function(from, to, message) {
    if (message.length > 0) {
      preq.get({
        uri: message
      }).then(function (res) {
        if (res.status === 200) {
          client.say(to, "It's just you.  " + message + ' is up.');
        } else {
          client.say(to, "It's not just you!  " + message + ' looks down from here.');
        }
      });
    } else {
      client.say(to, 'Usage: @down <uri>');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
