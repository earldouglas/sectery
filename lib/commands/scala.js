'use strict';

var preq = require('preq');

function listener(client, config) {
  function scala(to, code, client) {
    var encoded = encodeURIComponent(code);
    var req = {
      uri: 'http://www.simplyscala.com/interp?bot=irc&code=' + encoded,
      headers: config.headers,
    };

    preq.get(req).then(function (res) {
      if (res.headers && res.headers['set-cookie']) {
        var cookie = res.headers['set-cookie'];
        config.set('headers', 'cookie', cookie);
      }
      client.say(to, res.body);
    }).catch(function () {
      config.set('headers', {});
      req.headers = {};
      return preq.get(req);
    }).then(function (res) {
      client.say(to, res.body);
    }).catch(function (e) {
      console.error('@scala', e);
    });
  }

  return function(from, to, message) {
    if (message.length > 0) {
      scala(to, message, client);
    } else {
      client.say(to, 'Usage: @scala <expression>');
    }
  };

}

module.exports.event    = 'message';
module.exports.listener = listener;
