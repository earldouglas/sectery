'use strict';

function listener(client, config) {
  return function(from, message) {
    if (from === config.admin) {
      client.join(message);
      client.say(from, 'Joined ' + message + '.');
    }
  };
}

module.exports.event    = 'pm';
module.exports.listener = listener;
