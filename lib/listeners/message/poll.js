'use strict';

var util = require('../../utilities');
var say_slowly = require('../../say-slowly.js');

function initPoll(db,channel) {
  db.poll = db.poll || [];
  db.poll[channel] = db.poll[channel] || {id:0, polls:[]};
  console.log(db.poll[channel]);
  return db.poll[channel];
}

function usage(db,extraInfo,channel){

  var message = [];
  var channelPolls = {};
  if (extraInfo !== '') {
    message.push('Error: ' + extraInfo + ' ');
  }

  message.push('Usage: @poll <start|close> <message|id>');

  channelPolls = initPoll(db,channel);
  if (channelPolls.polls.length > 0 )  {
    
    channelPolls.polls.forEach(function(poll) {
      if (poll) {
        message.push('Id: ' + poll.id + ' User: ' + poll.user + ' Question: ' + poll.question + 
                     ' Yeas: ' + poll.yeas + ' Nays: ' + poll.nays+ ' Open: ' +  poll.isOpen);
      }
    });
  }
  return {callback: 
    function(say) {
      say_slowly(say, channel, message);
    }
  };
} 

function findPoll(polls,id) {
  var poll = polls.filter(function(poll) { 
  if (poll) 
  {
    return (poll.id === id);
  } 
  return false;
  });
  return poll[0] || null;
}

function voteListener(db, from, channel, message, messageCallback) {

  var match = /^@vote\s+(\d+)\s+(yea|nay)/.exec(message);
  var msg = '';
  var channelPolls = initPoll(db,channel);
  if (match) {

    var id  = parseInt(match[1]);
    var vote = match[2];
    var poll = findPoll(channelPolls.polls,id);
    
    if (poll)  {
      if (poll.isOpen) {
        if (vote === 'yea') 
          poll.yeas++;
        else if (vote === 'nay')
          poll.nays++;
        msg = from + ': OK - Voted ' + vote+' on Poll ' + id +'! Current votes: Yeas:' + poll.yeas + ' Nays:' + poll.nays;
      } else  {
        msg = from + ': Sorry - Poll ' + id + ' is already closed!';
      }
    } else {
        msg = from + ': Sorry - Poll ' + id + ' was not found.';
    }
  } else {
    msg = 'Usage: @vote <poll id> yea|nay';
  }
  return [ { to: channel, message: msg } ];

}
function pollListener(db, from, channel, message, messageCallback) {

  var match = /^@poll\s+((start)\s+(.*)|(close)\s+(\d+)|(delete)\s+(\d+))?$/.exec(message);
  var channelPolls = initPoll(db,channel);
  if (match) {
    var command = match[2] || match[4] || match[6];
    var question = match[3];   // for start 
    var id  = parseInt(match[5] || match[7]);  // for close

    var addPoll = function () {
      id = channelPolls.id;
      channelPolls.id = channelPolls.id + 1;
      return {callback: 
        function(say) {
          say(channel,from + ': OK - Poll ' + id + ' started!');
          channelPolls.polls.push({id: id, user: from, question: question, yeas:0, nays:0, isOpen:true,  date:util.now()});
        }
      };
    };

    var closePoll = function () {
      var message = '';

      var poll = findPoll(channelPolls.polls,id);
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
    
    var deletePoll = function () {
      var message = '';

      var poll = findPoll(channelPolls.polls,id);
      if (poll)  {
        var success = (!poll.isOpen);

        if (!success)  {
          if (poll.isOpen) 
            message = from + ': Sorry - Poll ' + id + ' is still open!';
        } else {
          message = from + ': OK - Poll ' + id + ' deleted!';
          delete channelPolls.polls[id];
        }
      
        return message;
      } else {
        return from + ': Sorry - Poll ' + id + ' was not found.';
      }
    };
    var commands = {'start':addPoll, 'close':closePoll, 'delete':deletePoll};
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
module.exports.help = [{ cmd:'@poll',
                         syntax: 'Usage: @poll start <message>',
                         output: {success: ['<user>: OK - Poll <id> started!'],
                                  failure: []}
                       },
                       { cmd:'@poll close',
                         syntax: 'Usage: @poll close <id>',
                         output: {success: ['<user>: OK - Poll <id> closed!'],
                                  failure: ['<user>: Sorry - Poll <id> is already closed!',
                                            '<user>: Sorry - Poll <id> can only be closed by "<user>"!']}
                       },
                       { cmd:'@poll delete',
                         syntax: 'Usage: @poll delete <id>',
                         output: {success: ['<user>: OK - Poll <id> delete!'],
                                  failure: ['<user>: Sorry - Poll <id> is still open!']}
                       },
                       { cmd:'@vote',
                         syntax: '@vote <poll id> yea|nay',
                         output: {success: ['<user>: OK - Voted yea|nay on Poll <id>! Current votes: Yeas: <yeas> Nays:<nays>'],
                                  failure: ['<user>: Sorry - Poll <id> is already closed!',
                                            '<user>: Sorry - Poll <id> can only be closed by "<user>"!']}
                       }];

