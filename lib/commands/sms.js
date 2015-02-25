'use strict';

var preq = require('preq');
var nconf = require('nconf');
var offline = false;
var uri = '';

function sms(to, message) {
  if (offline) {
    return;
  }
  preq.post({
    headers: {
      'content-type': 'application/x-www-form-urlencoded'
    },
    uri: uri,
    body: 'number=' + to + '&message=' + encodeURIComponent(message)
  }).catch(function (e) {
    console.error('@sms', e);
  });
};
function listener(client, config) {
  if (config.offline) {
    offline = true;
    sms = function () {};
  }
  uri = config.uri;
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
module.exports.sms = sms;
