'use strict';

function listener(client, config) {
  return function(message) {
    console.error('error:', message);
    client.say(config.admin, 'error: ' + message);
  };
}

module.exports.event    = 'error';
module.exports.listener = listener;
