'use strict';

var curl = require('../../curl');
var strip_ansi = require('strip-ansi');

function messageListener(db, from, channel, message, reply) {

  if (/^@wx/.test(message)) {
    var match = /^@wx\s+(.*)$/.exec(message);
    if (match) {
      var loc = match[1];

      curl( 'https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' + encodeURIComponent(loc),
            function (osmRes) {
              if (osmRes.length === 1) {

                curl( [ 'https://api.darksky.net/forecast/',
                         process.env.DARK_SKY_API_KEY,
                         '/',
                          osmRes[0].lat,
                         ',',
                         osmRes[0].lon
                      ].join(''),
                      function (dsRes) {
                        var summary =
                          [ 'Location: ', osmRes[0].display_name.split(', ')[0],
                            ', ', dsRes.currently.summary,
                            ', Temperature: ', dsRes.currently.temperature, '°',
                            ', Humidity: ', (dsRes.currently.humidity*100), '%',
                            ', Wind: ', dsRes.currently.windSpeed, ' mph',
                            ', Gusts: ', dsRes.currently.windGust, ' mph',
                            ', UV index: ', dsRes.currently.uvIndex,
                          ].join('');
                        reply({ to: channel, message: summary });
                      }
                    );

              } else {
                reply({ to: channel,
                        message: 'No location found for "' + loc + '"'
                      });
              }
            }
          );

    } else {
      reply({ to: channel, message: '@wx <location>' });
    }
  } 

}

module.exports = messageListener;
module.exports.help =
  [ { cmd:'@wx',
      syntax: '@wx <location>',
      output: { success: [ 'Current weather conditions' ], failure: [] }
    }
  ];
