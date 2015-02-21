'use strict';

var preq = require('preq');

function listener(client, config) {

  var sms = function (to, message) {
    preq.post({
      headers: {
        'content-type': 'application/x-www-form-urlencoded'
      },
      uri: config.uri,
      body: 'number=' + to + '&message=' + encodeURIComponent(message)
    }).catch(function (e) {
      console.error('@sms', e);
    });
  };

  if (config.offline) {
    sms = function () {};
  }

  return function(from, to, message) {
    if (message !== '') {
      if (config.contacts[from]) {
        client.say(to, 'I\'ll text you a reminder.');
        sms(config.contacts[from], message);
      } else {
        client.say(to, 'I don\'t know your phone number.');
      }
    } else {
      client.say(to, 'Usage: @sms <message>');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
