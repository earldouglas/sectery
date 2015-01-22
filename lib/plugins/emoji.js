'use strict';

function listener(client) {
  return function(from, to, message) {
    if (/table\s*flip/i.test(message)) {
      client.say(to, '╯°□°）╯︵ ┻━┻');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
