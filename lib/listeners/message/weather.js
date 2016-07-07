'use strict';

var curl = require('../../curl');
var strip_ansi = require('strip-ansi');

function messageListener(db, from, channel, message) {

  var messages = [];

  if (/^@weather/.test(message)) {
    var commandMatch = /^@weather\s+(.*)$/.exec(message);
    if (commandMatch) {
      var place = commandMatch[1];
      var url = 'http://wttr.in/' + place;
      var weather = curl(db,url).splice(1,6);
      for (var x = 0; x < 7; x++) {
        var message = strip_ansi(weather[x]);
        if (message && message.trim().length > 0) {
          messages.push({ to: channel, message: message});
        }
      }
    } else {
      messages.push({ to: channel, message: '@weather <location>'});
    }
  } 
  return messages;

}
module.exports = messageListener;

module.exports.help = [{ cmd:'@weather',
                         syntax: '@weather <place>',
                         output: {success: ['7 lines depicting the weather'],
                                  failure: []}
                       }];

