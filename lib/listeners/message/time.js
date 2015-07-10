'use strict';

var util = require('../../utilities.js');
var time = require('../../time.js');

function messageListener(db, from, channel, message) {
  var messages = [];
  if (/^@time$/.test(message)) {
        messages.push({ to: channel, message: util.now() });
  }
  if (/^@remind/.test(message)) {
      //@remind <minutes> <message>
    
    var matches = /^@remind\s+(\d+)\s+(.*)$/.exec(message);
    if (matches) {
      var minutes = parseInt(matches[1]);
      var message = matches[2];
      var now = util.now();
      messages.push(
      { callback: 
        function(say) {
          util.timeout(
            function() { 
              say(channel,from + ': Reminder: ' + message + ' ' + now);
            }, 
            time.time_delta(Date.now(),
                            new Date (Date.now() + (1000 * 60 * minutes)))
            );
        }
      });
    } else {
      messages.push({ to: channel, message: 'Usage: @remind <minutes> <message>' });
    }
  }
  return messages;
}

module.exports = messageListener;
