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

function messageListener(db, from, channel, message, reply) {

  if (/^@reply/.test(message)) {
    var commandMatch = /^@reply\s+delete\s+([.\-\w]+)$/.exec(message);
    var match = /^@reply\s+([.\-\w]+)\s+\/(.*)\/([ig]{0,2})\s+(.*)$/.exec(message);
    var replies = initReplies(db,channel);

    if (commandMatch) {
      var name = commandMatch[1];
      var index =  findReply(name,replies);

      if (index != replies.length) {
        replies.splice(index,1);
        reply({ to: channel, message: from + ': OK - auto-reply "' + name +'" removed.' });
      } else {
        reply({ to: channel, message: from + ': Sorry - auto-reply "' + name +'" not found.' });
      }
    } else if (match) {
      var name = match[1];
      var regex = match[2];
      var flags = match[3];
      var replyMessage = match[4]
      var index =  findReply(name,replies);

      if (index != replies.length) {
        replies.splice(index, 1);
      }

      replies[index] = { count: 10, name:name, regex: regex, flags: flags, reply: replyMessage, };
      reply({ to: channel, message: from + ': OK - auto-reply "' + name +'" added.' });
    } else {
      reply({ to: channel, message: '@reply <name> /<regex>/[ig] <reply>'});
      reply({ to: channel, message: '@reply delete <name>'});
      var list = [];
      for (var i = replies.length - 1; i >= 0; i--) {
        list.push(replies[i].name);
      }
      reply({ to: channel, message: 'Replies: '+ list.join(', ')});
    }
  } else {
    var replies = initReplies(db,channel);
    for (var i = replies.length - 1; i >= 0; i--) {
      var regex = new RegExp(replies[i].regex,replies[i].flags);
      if (regex.exec(message)) {
        reply({
          to: channel,
          message: replies[i].reply,
        });
        replies[i].count = replies[i].count || 10;
        replies[i].count = replies[i].count - 1;
        if (replies[i].count <= 0) {
          replies.splice(i, 1);
        }
      }
    }

  }
}
module.exports = messageListener;

module.exports.help = [{ cmd:'@reply',
                         syntax: '@reply <name> /<regex>/[ig] <reply>',
                         output: {success: ['<user>: OK - auto-reply "<name>" added.'],
                                  failure: []}
                       }];

