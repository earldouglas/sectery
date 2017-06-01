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

  var replies = initReplies(db,channel);

  if (/^@reply/.test(message)) {
    var commandMatch = /^@reply\s+delete\s+([.\-\w]+)$/.exec(message);
    var match = /^@reply\s+([.\-\w]+)\s+\/(.*)\/([ig]{0,2})\s+(.*)$/.exec(message);

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
      var replyName = match[1];
      var regex = match[2];
      var flags = match[3];
      var replyMessage = match[4];
      var replyIndex = findReply(replyName,replies);

      if (replyIndex != replies.length) {
        replies.splice(replyIndex, 1);
      }

      replies[replyIndex] = { count: 10, name:replyName, regex: regex, flags: flags, reply: replyMessage, };
      reply({ to: channel, message: from + ': OK - auto-reply "' + replyName +'" added.' });
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
    for (var k = replies.length - 1; k >= 0; k--) {
      var replyRegex = new RegExp(replies[k].replyRegex,replies[k].flags);
      if (replyRegex.exec(message)) {
        reply({
          to: channel,
          message: replies[k].reply,
        });
        replies[k].count = replies[k].count || 10;
        replies[k].count = replies[k].count - 1;
        if (replies[k].count <= 0) {
          replies.splice(k, 1);
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

