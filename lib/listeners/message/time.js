'use strict';

var util = require('../../utilities.js');
var time = require('../../time.js');
var moment = require('moment-timezone');

function getTimezone(db, from) {
  
  var timezone = 'America/Denver';

  if (db && db.settings && db.settings[from] && db.settings[from].tz)
    timezone = db.settings[from].tz;

  return timezone;
  
}
function messageListener(db, from, channel, message) {
  var messages = [];
  if (/^@time$/.test(message)) {

    var tz = getTimezone(db,from);

    var now = moment.tz(tz);
    var then = now.clone();
    then.hours(17);
    then.minutes(0);

    var prefix = from  + ': ' + now.format('h:mm A z [on] ddd[,] MMM Do') + ', ';
    var weekdays = [1,2,3,4,5];
    if (weekdays.indexOf(now.day()) != -1 ) {
      if (now.valueOf() >= then.valueOf()) {
        messages.push({ to: channel, message: prefix + 'why are you still here? Go home.'});
      } else {
        messages.push({ to: channel, message: prefix + time.time_delta(now,then) + ' until you get to go home. Hang in there.' });
      }
    } else {
        messages.push({ to: channel, message: prefix +  'enjoy your day of not-work.'});
    }
  }
  if (/^@remind/.test(message)) {
      //@remind <minutes> <message>
    
    var matches = /^@remind\s+(\d+)\s+(.*)$/.exec(message);
    if (matches) {
      var minutes = parseInt(matches[1]);
      var message = matches[2];
      var now = util.now();

      messages.push( { to: channel, message: from + ': reminder added.' });
      messages.push( {
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

module.exports.help = [{ cmd:'@time',
                         syntax: '@time',
                         output: {success: [],
                                  failure: []}
                       },
                       { cmd:'@remind',
                         syntax: '@remind <minutes> <message>',
                         output: {success: ['<from>: Reminder: <message> <date time>'],
                                  failure: []}
                       }
                       ];
