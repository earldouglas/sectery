'use strict';

var util = require('../../utilities');
var say_slowly = require('../../say-slowly.js');

function initPoll(db) {
    db.poll = db.poll || {};
    db.poll.polls = db.poll.polls || [];
}

function usage(db,extraInfo,channel){

  var message = '';
  if (extraInfo !== '') {
    message = 'Error: ' + extraInfo + ' '; 
  }

  message = message + 'Usage: @poll <start|close> <message|id>';

  initPoll(db);
  if (db.poll.polls.length > 0 )  {
    
    message = message + '\n';
    message = message  +
      'Id  User         Question                   Ayes Noes   Status\n';
    db.poll.polls.forEach(function(poll) {
      message = message + poll.id + ' | ' + poll.user + ' | ' + poll.question + 
                ' | ' + poll.ayes + ' | ' + poll.noes + ' | ' +  poll.isOpen + '\n';
    });
  }
  return {callback: 
    function(say) {
      say(channel,message);
    }
  };
} 

function messageListener(db, from, channel, message, messageCallback) {
  var messages = [];

  if (/^@poll/.test(message)) {
    var match = /^@poll\s+((start)\s+(.*)|(close)\s+(\d+))?$/.exec(message);
    initPoll(db);
    if (match) {
      var command = match[2] || match[4]; 
      var question = match[3];   // for start 
      var id  = parseInt(match[5]);     // for close

      var addPoll = function () {
        db.poll.id = db.poll.id || 0;
        id = db.poll.id;
        db.poll.id = db.poll.id + 1;
        return {callback: 
          function(say) {
            say(channel,from + ': OK - Poll ' + id + ' started!');
            db.poll.polls.push({id: id, user: from, question: question, ayes:0, noes:0, isOpen:true,  date:util.now()});
          }
        };
      };

      var closePoll = function () {
        var message = '';
        db.poll = db.poll || {};

        var polls = db.poll.polls.filter(function(poll) { return (poll.id === id);});
        if (polls.length === 1)  {
          var success = (polls[0].isOpen && (polls[0].user == from));

          if (!success)  {
            if (!polls[0].isOpen) 
              message = from + ': Sorry - Poll ' + id + ' is already closed!';
            else if (polls[0].user !== from)
              message = from + ': Sorry - Poll ' + id + ' can only be closed by "'+ polls[0].user + '"!';
          } else {
            message = from + ': OK - Poll ' + id + ' closed!';
            polls[0].isOpen = false;
          }
        
          return message;
        } else {
          return from + ': Sorry - Poll ' + id + ' was not found.';
        }
      };
      
      var commands = {'start':addPoll, 'close':closePoll};
      var msg = usage(db,'',channel);
      if (commands[command]) {
        msg = commands[command]();
      }
      if (msg.callback) {
        return [ msg ];
      }
      return [ { to: channel, message: msg } ];
    } // not a match
    return [ usage(db,'',channel)];
  } //not a command.
  return messages;
}

module.exports = messageListener;
