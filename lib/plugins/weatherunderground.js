'use strict';

var preq = require('preq');

function listener(client, config) {
  return function(from, to, message) {
    if (/^@wu/.test(message)) {
      var match = /^@wu\s+(.+)$/.exec(message);
      if (match) {
        preq.get({
          uri:  config.autocomplete.uri +
                encodeURIComponent(match[1])
          
        }).then(function(res) {
          var json = JSON.parse(res.body)
          var url = config.api.uri + config.api.key + '/conditions/' + json['RESULTS'][0].l + '.json'
          console.log(url)
          preq.get({
            uri: url
          }).then(function(res) {
            client.say(to, 'Temp: ' + res.body.current_observation.temp_f + 'F' +
              ', humidity: ' + res.body.current_observation.relative_humidity +
              ', weather: ' + res.body.current_observation.weather)
          });

        });
      } else {
        client.say(to, 'Usage: @wu <location>');
      }
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
