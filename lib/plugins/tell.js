'use strict';

function listener(client, config) {
  return function(from, to, message) {
    if (config.messages[from]) {
      config.messages[from].forEach(function (msg) {
        client.say(msg.channel, msg.to + ': ' + msg.from +
                   ' said ' + msg.message);
      });
      config.clear('messages', from);
    }
    if (/^@tell/.test(message)) {
      var match = /^@tell\s+(\w+)\s+(.+)$/.exec(message);
      if (match) {
        var who = match[1];
        var messages = config.messages[who] || [];
        messages.push({
          channel: to,
          to: who,
          from: from,
          message: match[2]
        });
        config.set('messages', who, messages);
        client.say(to, 'I\'ll pass your message along.');
      } else {
        client.say(to, 'Usage: @tell <username> <message>');
      }
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
