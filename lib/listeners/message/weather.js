'use strict';

var preq = require('preq');

function temp(res) {
  if (res && res.body && res.body.current_observation &&
      res.body.current_observation.temp_f) {
    return res.body.current_observation.temp_f + 'F';
  } else {
    return 'unknown';
  }
}

function humidity(res) {
  if (res && res.body && res.body.current_observation &&
      res.body.current_observation.relative_humidity) {
    return res.body.current_observation.relative_humidity;
  } else {
    return 'unknown';
  }
}

function weather(res) {
  if (res && res.body && res.body.current_observation &&
      res.body.current_observation.weather) {
    return res.body.current_observation.weather;
  } else {
    return 'unknown';
  }
}

function autocomplete(location, config) {
  var url = config.autocompleteUri + encodeURIComponent(location);
  return preq.get({ uri:url });
}

function getweather(feature, location, config) {
  return autocomplete(location, config).then(function(res) {
    var json = JSON.parse(res.body);
    var url = config.apiUri + config.apiKey + '/' + feature + '/' +
              json.RESULTS[0].l + '.json';
    return preq.get({ uri: url });
  });
}

function forecast(res) {
  if (res && res.body && res.body.forecast &&
      res.body.forecast.txt_forecast &&
      res.body.forecast.txt_forecast.forecastday) {
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

function messageListener(db, from, channel, message) {
  db.weather = db.weather || {};
  var match = /^@weather\s+(\w+)\s*(.*)$/.exec(message);
  if (match) {
    if (match[1] === 'forecast') {
      getweather(match[1], match[2], db.weather).then(function(res) {
        var fc = forecast(res);
        for (var i = 0; i < 8; i++) {
          return [{
            to: channel,
            message: forecastTitle(fc[i]) + ' : ' + forecastText(fc[i])
          }];
        }
      });
    } else {
      getweather('conditions', message, db.weather).then(function(res) {
        return [{
          to: channel,
          message: [ 'Temp: ' + temp(res),
                     'humidity: ' + humidity(res),
                     'weather: ' + weather(res) ].join(', ')
        }];
      });
    }
  } else {
    return [{
      to: channel,
      message: 'Usage: @weather [forecast] <location>'
    }];
  }
}

module.exports = messageListener;
