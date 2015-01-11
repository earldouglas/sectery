'use strict';

var preq = require('preq');

function listener(client, config) {

  function sms(to, message) {
    preq.post({
      headers: {
        'content-type': 'application/x-www-form-urlencoded'
      },
      uri: config.uri,
      body: 'number=' + to + '&message=' + encodeURIComponent(message)
    }).catch(function (e) {
      console.error('@sms', e);
    });
  }

  return function(from, to, message) {
    if (/^@sms/.test(message)) {
      var match = /^@sms\s+(.+)$/.exec(message);
      if (match) {
        if (config.contacts[from]) {
          client.say(to, 'I\'ll text you a reminder.');
          sms(config.contacts[from], match[1]);
        } else {
          client.say(to, 'I don\'t know your phone number.');
        }
      } else {
        client.say(to, 'Usage: @sms <message>');
      }
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
