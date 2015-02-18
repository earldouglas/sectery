'use strict';
function queueMessage(config, channel, who, from, message) {
  var messages = config.messages[who] || [];
  messages.push({
    channel: channel,
    to: who,
    from: from,
    message: message
  });
  return messages;
}
function listener(client, config) {
  return function(from, channel, message) {
    
    if (config.messages[from]) {
      
      config.messages[from].forEach(function (msg) {
        client.say(msg.channel, msg.to + ': ' + msg.from +
                   ' said ' + msg.message);
      });
      delete config.messages[from];
      config.clear('messages', from);
    }
    if (/^@tell/.test(message)) {
      var match = /^@tell\s+(\w+|\*)\s+(.+)$/.exec(message);
      if (match) {
        var who = match[1];
        if (who === '*') {

          config.getNicknames(channel).forEach(function(nickname) {
            if (from !== nickname) {
              var messages = queueMessage(config,channel,nickname,from,match[2]);
              
              config.messages[nickname] = messages;
              config.set('messages', nickname, messages);
            }
          });

        } else {
          var messages = queueMessage(config,channel,who,from,match[2]);
          config.messages[who] = messages;
          config.set('messages', who, messages);

        }
        client.say(channel, 'I\'ll pass your message along.');
      } else {
        client.say(channel, 'Usage: @tell <username> <message>');
      }
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
