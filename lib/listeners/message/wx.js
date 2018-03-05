'use strict';

var curl = require('../../curl');
var strip_ansi = require('strip-ansi');

function messageListener(db, from, channel, message, reply) {

  if (/^@wx/.test(message)) {
    var match = /^@wx\s+(.*)$/.exec(message);
    if (match) {
      var loc = match[1];

      curl( 'https://nominatim.openstreetmap.org/search/'
          + encodeURIComponent(loc)
          + '?format=json&limit=1'
          , function (osmRes) {
              if (osmRes.length === 1) {

                curl( 'https://api.darksky.net/forecast/'
                    + process.env['DARK_SKY_API_KEY']
                    + '/'
                    + osmRes[0].lat
                    + ','
                    + osmRes[0].lon
                    , function (dsRes) {
                        var summary =
                          [ osmRes[0].display_name.split(', ')[0]
                          , ': ', dsRes.currently.temperature, '°'
                          , ', ', dsRes.currently.humidity, '%'
                          , ', ', dsRes.currently.windSpeed
                          , ' (', dsRes.currently.windGust, ') mph'
                          , ', ', dsRes.currently.uvIndex, ' UV'
                          ].join('');
                        reply({ to: channel , message: summary });
                      }
                    );

              } else {
                reply({ to: channel
                      , message: 'No location found for "' + loc + '"'
                      });
              }
            }
          );

    } else {
      reply({ to: channel, message: '@wx <location>' });
    }
  } 

}

// messageListener({}, 'frommmm', 'channn', '@wx louisville, co', function (r) { console.log(r); });

module.exports = messageListener;
module.exports.help =
  [ { cmd:'@wx'
    , syntax: '@wx <location>'
    , output: { success: [ 'Current weather conditions' ]
              , failure: []
              }
    }
  ];
