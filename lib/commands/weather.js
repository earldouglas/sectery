'use strict';

var preq = require('preq');

function temp(res) {
  if (res && res.body && res.body.current_observation && res.body.current_observation.temp_f) {
    return res.body.current_observation.temp_f + 'F';
  } else {
    return 'unknown';
  }
}

function humidity(res) {
  if (res && res.body && res.body.current_observation && res.body.current_observation.relative_humidity) {
    return res.body.current_observation.relative_humidity;
  } else {
    return 'unknown';
  }
}

function weather(res) {
  if (res && res.body && res.body.current_observation && res.body.current_observation.weather) {
    return res.body.current_observation.weather;
  } else {
    return 'unknown';
  }
}

function listener(client, config) {
  return function(from, to, message) {
    if (message !== '') {
      preq.get({ uri:  config.autocomplete.uri + encodeURIComponent(message) })
      .then(function(res) {
        var json = JSON.parse(res.body);
        var url = config.api.uri + config.api.key + '/conditions/' + json.RESULTS[0].l + '.json';
        return preq.get({ uri: url });
      })
      .then(function(res) {
        client.say(to, 'Temp: ' + temp(res) + ', humidity: ' + humidity(res) +
                       ', weather: ' + weather(res));
      });
    } else {
      client.say(to, 'Usage: @weather <location>');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
