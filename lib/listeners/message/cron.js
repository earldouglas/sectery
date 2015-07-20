'use strict';

var util = require('../../utilities');
var CronJob = require('cron').CronJob;

function usage(extraInfo){
  var message = '';
  if (extraInfo !== '') { message = 'Error: ' + extraInfo + '. ';
  }
  return message + 'Usage: @cron <add|remove> <cron-string> <message>|<id>';
}
function cronPatternValid(cronPattern) {
  var result = true;
  try {
    new CronJob(cronPattern, function() {
      console.log('cron pattern valid');
    });
  } catch(ex) {
    result = false;
    console.log("cron pattern not valid");
  }
  return result;
}
function messageListener(db, from, channel, message) {
  
  var messages = [];

  //
  if (/^@cron/.test(message)) {
    var match = /^@cron\s+((add)\s+"(.*)"\s+(.+)|(remove)\s+(\d+))$/.exec(message);
    db.cron = db.cron || [];

    if (match) {
      console.log(match);
      var cmd = match[2] | match[5];
      var cronString = match[3]; // will be undefined for remove
      var message = match[4];    // will be undefined for remove
      var id = match[6];         // will be undefined for add

      var addCron = function () {
        if (!cronPatternValid(cronString)) {
          return usage(from + ': The cron string "' + cronString + '" is not valid.');
        }
        
        // TODO - get a unique ID system
        var id = 0;

        return {callback: 
          function(say) {

            var job = new CronJob(cronString,
                                  function() { // scheduled function
                                    say(message);
                                  }, 
                                  function() { // on Stop
                                    say(from + ': OK - cron job ' + id + ' stopped!');
                                  },
                                  true, // don't start right away
                                  'Americas/Los_Angeles'); //timezone

            say(from + ': OK - cron job ' + id + ' scheduled!');
            db.cron.push({id: id, job: job, date:util.now()});
          }
        };
      };

      var removeCron = function () {
        if (db.cron[id]) {
          db.cron[id].stop();
          delete db.cron[id];
        } else {
          return usage(from + ': The cron job with id "' + id + '" was not found.');
        }
      };
      
      var commands = {'add':addCron, 'remove':removeCron};
      var msg = usage('');
      if (commands[command]) {
        msg = commands[command]();
      }
      if (msg.callback) {
        return [ msg.callback ];
      }
      return [ { to: from, message: msg } ];
    } // not a match
    return [ { to: from, message: usage('')} ];
  } //not a cron command.
  return messages;
}

module.exports = messageListener;
