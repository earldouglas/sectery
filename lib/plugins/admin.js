'use strict';

function listener(client, config) {
  return function(from, message) {
    if (from === config.admin) {
      var cmd = message.trim().split(' ');
      if (cmd[0] === '@join' && cmd.length === 2) {
        var join = cmd[1];
        client.join(join);
        client.say(from, 'Joined ' + join + '.');
      } else if (cmd[0] === '@part' && cmd.length === 2) {
        var part = cmd[1];
        client.part(part);
        client.say(from, 'Left ' + part + '.');
      }
    }
  };
}

module.exports.event    = 'pm';
module.exports.listener = listener;
