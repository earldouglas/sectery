'use strict';

function listener(client, config) {
  return function(from, message) {
    if (from === config.admin) {
      client.part(message);
      client.say(from, 'Left ' + message + '.');
    }
  };
}

module.exports.event    = 'pm';
module.exports.listener = listener;
