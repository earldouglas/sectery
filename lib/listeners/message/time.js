'use strict';

var util = require('../../utilities.js');
var time = require('../../time.js');

function messageListener(db, from, channel, message) {
  var messages = [];
  if (/^@time$/.test(message)) {
    var now = Date.now();
    messages.push({ to: channel, message: util.now() + ', ' + time.next_end_work_day() + ' until end of next workday.'});
  }
  if (/^@remind/.test(message)) {
      //@remind <minutes> <message>
    
    var matches = /^@remind\s+(\d+)\s+(.*)$/.exec(message);
    if (matches) {
      var minutes = parseInt(matches[1]);
      var message = matches[2];
      var now = util.now();

      messages.push(
      { to: channel, message: from + ': reminder added.' ,
        callback: 
        function(say) {
          util.timeout(
            function() { 
              say(channel,from + ': Reminder: ' + message + ' ' + now);
            }, 
            Math.abs(new Date().getTime() -
                     new Date (Date.now() + (1000 * 60 * minutes))) // difference in ms of the given time.
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
