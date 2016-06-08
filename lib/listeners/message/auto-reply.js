'use strict';

var util = require('../../utilities');

function initReplies(db,channel) {
  if (Array.isArray(db.replies))  {
    db.replies = {};
  } else {
    db.replies = db.replies || {};
  }
  
  db.replies[channel] = db.replies[channel] || []; 
  return db.replies[channel];
}
function findReply(toFindName, replies) {
  for (var i = replies.length - 1; i >= 0; i--) {
    var name = replies[i].name;
    var regex = replies[i].regex;
    var reply = replies[i].reply;

    if (name === toFindName)
    {
      return i;
    }
  }
  return replies.length;
}

function messageListener(db, from, channel, message) {

  var messages = [];

  if (/^@reply/.test(message)) {
    var commandMatch = /^@reply\s+delete\s+([.\-\w]+)$/.exec(message);
    var match = /^@reply\s+([.\-\w]+)\s+\/(.*)\/([ig]{0,2})\s+(.*)$/.exec(message);
    var replies = initReplies(db,channel);

    if (commandMatch) {
      var name = commandMatch[1];
      var index =  findReply(name,replies);

      if (index != replies.length) {
        replies.splice(index,1);
        messages.push({ to: channel, message: from + ': OK - auto-reply "' + name +'" removed.' });
      } else {
        messages.push({ to: channel, message: from + ': Sorry - auto-reply "' + name +'" not found.' });
      }
    } else if (match) {
      var name = match[1];
      var regex = match[2];
      var flags = match[3];
      var reply = match[4]
      var index =  findReply(name,replies);

      if (index != replies.length) {
        delete replies[index]
      }

      replies[index] = {name:name, regex: regex, flags: flags, reply: reply};
      messages.push({ to: channel, message: from + ': OK - auto-reply "' + name +'" added.' });
    } else {
      messages.push({ to: channel, message: '@reply <name> /<regex>/[ig] <reply>'});
      messages.push({ to: channel, message: '@reply delete <name>'});
      var list = [];
      for (var i = replies.length - 1; i >= 0; i--) {
        list.push(replies[i].name);
      }
      messages.push({ to: channel, message: 'Replies: '+ list.join(', ')});
    }
    return messages;
  } else {
    var replies = initReplies(db,channel);
    for (var i = replies.length - 1; i >= 0; i--) {
      var regex = new RegExp(replies[i].regex,replies[i].flags);
      var reply = replies[i].reply;
      if (regex.exec(message)) {
        messages.push({
          to: channel,
          message: reply
        });
      }
    }
    return messages;

  }
}
module.exports = messageListener;

module.exports.help = [{ cmd:'@reply',
                         syntax: '@reply <name> /<regex>/[ig] <reply>',
                         output: {success: ['<user>: OK - auto-reply "<name>" added.'],
                                  failure: []}
                       }];

