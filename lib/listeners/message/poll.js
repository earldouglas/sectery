'use strict';

var util = require('../../utilities');
var say_slowly = require('../../say-slowly.js');

function initPoll(db) {
    db.poll = db.poll || {};
    db.poll.polls = db.poll.polls || [];
}

function usage(db,extraInfo,channel){

  var message = [];
  if (extraInfo !== '') {
    message.push('Error: ' + extraInfo + ' ');
  }

  message.push('Usage: @poll <start|close> <message|id>');

  initPoll(db);
  if (db.poll.polls.length > 0 )  {
    
    message.push('');
    db.poll.polls.forEach(function(poll) {
    message.push('Id: ' + poll.id + ' User: ' + poll.user + ' Question: ' + poll.question + 
                ' Ayes: ' + poll.ayes + ' Noes: ' + poll.noes + ' Open: ' +  poll.isOpen);
    });
  }
  return {callback: 
    function(say) {
      say_slowly(say, channel, message);
    }
  };
} 

function findPoll(db,id) {
  var polls = db.poll.polls.filter(function(poll) { return (poll.id === id);});
  return polls[0] || null;
}

function voteListener(db, from, channel, message, messageCallback) {

  var match = /^@vote\s+(\d+)\s+(aye|nay)/.exec(message);
  var msg = '';
  initPoll(db);
  if (match) {

    var id  = parseInt(match[1]);
    var vote = match[2];
    var poll = findPoll(db,id);
    
    if (poll)  {
      if (poll.isOpen) {
        if (vote === 'aye') 
          poll.ayes++;
        else if (vote === 'nay')
          poll.nays++;
        msg = from + ': OK - Voted ' + vote+' on Poll ' + id +'! Current votes: ayes:' + poll.ayes + ' noes:' + poll.noes;
      } else  {
        msg = from + ': Sorry - Poll ' + id + ' is already closed!';
      }
    } else {
        msg = from + ': Sorry - Poll ' + id + ' was not found.';
    }
  } else {
    msg = from + ': Usage: @vote <poll id> aye|no';
  }
  return [ { to: channel, message: msg } ];

}
function pollListener(db, from, channel, message, messageCallback) {

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

      var poll = findPoll(db,id);
      if (poll)  {
        var success = (poll.isOpen && (poll.user == from));

        if (!success)  {
          if (!poll.isOpen) 
            message = from + ': Sorry - Poll ' + id + ' is already closed!';
          else if (poll.user !== from)
            message = from + ': Sorry - Poll ' + id + ' can only be closed by "'+ poll.user + '"!';
        } else {
          message = from + ': OK - Poll ' + id + ' closed!';
          poll.isOpen = false;
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

}

function messageListener(db, from, channel, message, messageCallback) {
  var messages = [];

  if (/^@poll/.test(message))
    return pollListener(db, from, channel, message, messageCallback);
  else if (/^@vote/.test(message))
    return voteListener(db, from, channel, message, messageCallback);
  else //not a command.
  return messages;
}

module.exports = messageListener;
