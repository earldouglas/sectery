'use strict';

var util = require('../../utilities');
var CronJob = require('cron').CronJob;

var jobs = [];

function usage(extraInfo){
  var message = '';
  if (extraInfo !== '') {
    message = 'Error: ' + extraInfo + ' '; 
  }
  return message + 'Usage: @cron <add|remove> "<cron-string>" <message>|<id>';
} 

function cronPatternValid(cronPattern) { var result = true; try {
    new CronJob(cronPattern, function() {
    });
  } catch(ex) {
    result = false;
  }
  return result;
}

function messageListener(db, from, channel, message, messageCallback) {
  
  var messages = [];

  //
  if (/^@cron/.test(message)) {
    var match = /^@cron\s+((add)\s+"(.*)"\s+(.+)|(remove)\s+(\d+))$/.exec(message);
    db.cron = db.cron || [];

    if (match) {
      var command = match[2] || match[5];
      var cronString = match[3]; // will be undefined for remove
      var message = match[4];    // will be undefined for remove
      var id = match[6];         // will be undefined for add

      var addCron = function () {
        if (!cronPatternValid(cronString)) {
          return usage(from + ': The cron string "' + cronString + '" is not valid.');
        }
        
        // TODO - get a unique ID system
        var id = jobs.length;

        return {callback: 
          function(say) {
            var job = new CronJob(cronString,
                                  function() { // scheduled function

                                    say(channel,message);

                                    //call the callback so that the message listener can parse the command
                                    messageCallback(from,channel,message);
                                  }, 
                                  function() { // on Stop
                                    say(channel,from + ': OK - cron job ' + id + ' stopped!');
                                  },
                                  true, // don't start right away
                                  'America/Los_Angeles', //timezone
                                  this);

            jobs[id] = job;
            say(channel,from + ': OK - cron job ' + id + ' scheduled!');
            db.cron[id] = {id: id, cronString: cronString, command: message, date:util.now()};
          }
        };
      };

      var removeCron = function () {

        if (db.cron[id]) {
          jobs[id].stop();
          delete db.cron[id];
          delete jobs[id];
          return '';
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
        return [ msg ];
      }
      return [ { to: channel, message: msg } ];
    } // not a match
    return [ { to: channel, message: usage('')} ];
  } //not a cron command.
  return messages;
}

module.exports = messageListener;
