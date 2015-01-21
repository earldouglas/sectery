'use strict';

function listener(client, config) {
  return function(from, to, message) {
    if (/Isn'?t that right,? sectery[?]?$/i.test(message)) {
      client.say(to, config.agreement + ', ' + from + '.  ' + config.agreement + '.');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
