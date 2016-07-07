'use strict';

var curl = require('../../curl');

function messageListener(db, from, channel, message) {

  var messages = [];

  if (/^@weather/.test(message)) {
    var commandMatch = /^@weather\s+(.*)$/.exec(message);
    if (commandMatch) {
      var place = commandMatch[1];
      var url = 'http://wttr.in/' + place;
      var weather = curl(db,url).splice(1,6);
      for (var x = 0; x < 7; x++) {
        if (weather[x] && weather[x].trim().length > 0) {
          messages.push({ to: channel, message: weather[x]});
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

