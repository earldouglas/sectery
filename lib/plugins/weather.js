'use strict';

var preq = require('preq');

function ktof(k) {
  var f = (k - 273.15) * 1.8000 + 32.00; 
  return Math.round(f * 10) / 10;
}

function listener(client) {
  return function(from, to, message) {
    if (/^@weather/.test(message)) {
      var match = /^@weather\s+(.+)$/.exec(message);
      if (match) {
        preq.get({
          uri: 'http://api.openweathermap.org/data/2.5/weather?q=' +
                encodeURIComponent(match[1])
        }).then(function (res) {
          client.say(to, 'Temp: ' + ktof(res.body.main.temp) + 'F' +
                         ', humidity: ' + res.body.main.humidity + '%' +
                         ', high: ' + ktof(res.body.main.temp_max) + 'F' +
                         ', low: ' + ktof(res.body.main.temp_min) + 'F'
          );
        });
      } else {
        client.say(to, 'Usage: @weather <location>');
      }
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
