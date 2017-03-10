'use strict';

var util = require('../../utilities.js');
var time = require('../../time.js');

function messageListener(db, from, channel, message) {
  var messages = [];
  if (/^@time$/.test(message)) {

    var now = util.now();

    var then = new Date();
    then.setHours(17);
    then.setMintues(0);

    
    if ([1..5].indexOf(now.getDay()) ) {
      if (now > then) {
        messages.push({ to: channel, message: from  + ': it\'s ' + util.now() + ' why are you still here? Go home.'});
      } else {
        messages.push({ to: channel, message: from  + ': ' + time.time_delta(now,then) + ' until you get to go home. Hang in there.' });
      }
    } else {
        messages.push({ to: channel, message: from  + ': it\'s ' + util.now() + 'enjoy your day of not-work.'});
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
                         output: {success: ['<date time>, <days, hours, min, sec> until end of next workday.'],
                                  failure: []}
                       },
                       { cmd:'@remind',
                         syntax: '@remind <minutes> <message>',
                         output: {success: ['<from>: Reminder: <message> <date time>'],
                                  failure: []}
                       }
                       ];
