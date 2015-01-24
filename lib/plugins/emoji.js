'use strict';

function listener(client, config) {
  return function(from, to, message) {
    Object.keys(config).forEach(function (k) {
      var p = new RegExp(k);
      if (p.test(message)) {
        client.say(to, config[k]);
      }
    });
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
