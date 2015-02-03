'use strict';

function listener(client) {
  return function(from, to, message) {
    client.say(to, (new Date()).toString());
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
