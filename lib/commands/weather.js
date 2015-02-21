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

function autocomplete(location,config) {
  var url = config.autocomplete.uri + encodeURIComponent(location);
  return preq.get({ uri:url });
}
function getweather(feature,location,config) {
  return autocomplete(location,config).then(function(res) {
    var json = JSON.parse(res.body);
    var url = config.api.uri + config.api.key + '/' + feature + '/' + json.RESULTS[0].l + '.json';
    return preq.get({ uri: url });
  });
}
function forecast(res) {
  if (res && res.body && res.body.forecast && res.body.forecast.txt_forecast && res.body.forecast.txt_forecast.forecastday) {
    return res.body.forecast.txt_forecast.forecastday;
  } else {
    return 'unknown';
  }
}
function forecastTitle(res) {
  if (res && res.title) {
    return res.title;
  } else {
    return 'unknown';
  }
}
function forecastText(res) {
  if (res && res.fcttext) {
    return res.fcttext;
  } else {
    return 'unknown';
  }
}

function listener(client, config) {
  return function(from, to, message) {

    var match = /^(\w+)\s*(.*)$/.exec(message);
    if (match) {
      if (match[1] === 'forecast') {
        getweather(match[1],match[2],config).then(function(res) {
          var fc = forecast(res);
          for (var i = 0; i < 8; i++) {
            client.say(to, forecastTitle(fc[i]) + ' : ' + forecastText(fc[i]));
          }
        });
      } else {
        getweather('conditions',message, config).then(function(res) {
          client.say(to, 'Temp: ' + temp(res) + ', humidity: ' + humidity(res) +
            ', weather: ' + weather(res));
        });
      }
    } else {
      client.say(to, 'Usage: @weather [forecast] <location>');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
