'use strict';

var curl = require('../../curl');
var strip_ansi = require('strip-ansi');

function messageListener(db, from, channel, message, reply) {

  if (/^@weather/.test(message)) {
    var commandMatch = /^@weather\s+(.*)$/.exec(message);
    if (commandMatch) {
      var place = commandMatch[1];
      curl('http://wttr.in/' + place, function (res) {
           console.log('weather response:', res);
              var weather = res.split('\n').splice(1,6);
              for (var x = 0; x < 7; x++) {
                var message = strip_ansi(weather[x]);
                if (message && message.trim().length > 0) {
                  reply({ to: channel, message: message});
                }
              }
            });
    } else {
      reply({ to: channel, message: '@weather <location>' });
    }
  } 

}

module.exports = messageListener;
module.exports.help = [{ cmd:'@weather',
                         syntax: '@weather <place>',
                         output: {success: ['7 lines depicting the weather'],
                                  failure: []}
                       }];
