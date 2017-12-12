'use strict';

var util = require('../../utilities.js');
var time = require('../../time.js');
var moment = require('moment-timezone');

function messageListener(db, from, channel, message, reply) {
  if (/^@time$/.test(message)) {

    var tz = time.getTimezone(db,from);

    var timeNow = moment.tz(tz);
    var then = timeNow.clone();
    then.hours(17);
    then.minutes(0);

    var prefix = from  + ': ' + timeNow.format('h:mm A z [on] ddd[,] MMM Do') + ', ';
    var weekdays = [1,2,3,4,5];
    if (weekdays.indexOf(timeNow.day()) != -1 ) {
      if (timeNow.valueOf() >= then.valueOf()) {
        reply({ to: channel, message: prefix + 'why are you still here? Go home.'});
      } else {
        reply({ to: channel, message: prefix + time.time_delta(timeNow,then) + ' until you get to go home. Hang in there.' });
      }
    } else {
        reply({ to: channel, message: prefix +  'enjoy your day of not-work.'});
    }
  }
  if (/^@remind/.test(message)) {
      //@remind <minutes> <message>
    
    var matches = /^@remind\s+(\d+)\s+(.*)$/.exec(message);
    if (matches) {
      var minutes = parseInt(matches[1]);
      var remindMessage = matches[2];
      var remindNow = util.now();

      reply( { to: channel, message: from + ': reminder added.' });

      util.timeout(
        function() { 
          reply({ to: channel, message: from + ': Reminder: ' + remindMessage + ' ' + remindNow });
        }, 
        Math.abs(new Date().getTime() -
                 new Date (Date.now() + (1000 * 60 * minutes))) // difference in ms of the given time.
      );
    } else {
      reply({ to: channel, message: 'Usage: @remind <minutes> <message>' });
    }

  }
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
