'use strict';

var util = require('../../utilities');
var say_slowly = require('../../say-slowly.js');

function getChannelList(db,channel) {
  db.list = db.list|| {};
  db.list[channel] = db.list[channel] || {};
  return db.list[channel];
}

function getListName(db,channel,listName) {
  var list = getChannelList(db,channel);
  list[listName] = list[listName] || [];
  return list[listName]
}

function findListItemIndex(list,listItem) {
  for (var i = 0; i < list.length; i++) {
    if (listItem === list[i])
      return i;
  }
  return list.length
}

function usage(db,channel){

  var message = [];

  message.push('Usage: @list <list-name>');
  message.push('Usage: @list <list-name> <add|delete> <list-item>');

  var list = getChannelList(db,channel);
  if (Object.keys(list).length > 0)
    message.push('available lists: ' + Object.keys(list).join(' '));

  return [{
    callback: function(say) {
      say_slowly(say, channel, message);
    }
  }];

} 

function addListItem(db,from,channel,listName,listItem) {
  var list = getListName(db,channel,listName);
  list.push(listItem);
}

function deleteListItem(db,from,channel,listName, listItem) {
  var list = getListName(db,channel,listName);
  var index = findListItemIndex(list,listItem);
  if (list[index])
    list.splice(index,1);

}

function generateMsg(db,from,channel,listName,command,listItem) {

  if (command == 'add')
    return {msg: 
            from + ': OK - "'+listItem+'" was added to '+listName+'.',
            success: true};

  if (command == 'delete') {

    var list = getChannelList(db,channel);
    var list = list[listName];
    if (list) {
      var index = findListItemIndex(list,listItem);
       if (list[index]) {
         return {msg: 
           from + ': OK - "'+listItem+'" was deleted from '+listName+'.',
             success: true};
       } else {
         return {msg: 
           from + ': Sorry - "'+listItem+'" was not found in '+listName+'.',
             success: false};
       }
    } else {
         return {msg: 
           from + ': Sorry - "'+listName+'" was not found.',
             success: false};
    }
  }

}
function listListener(db, from, channel, message, messageCallback) {

  var listMatch= /^@list\s+([_|\-,\w]+)$/.exec(message);
  var cmdMatch= /^@list\s+([_|\-,\w]+)\s+(add|delete)\s+(.*)$/.exec(message);
  var msg = '';

  if (listMatch) {

  } else if (cmdMatch) {
    var commands = { 'add':addListItem, 'delete':deleteListItem };
    var listName = cmdMatch[1];
    var command = cmdMatch[2];
    var listItem = cmdMatch[3]

    msg = generateMsg(db,from,channel,listName,command,listItem)
    if(msg.success) {
      commands[command](db,from,channel,listName,listItem);
    }

  } else {
    return usage(db,channel);
  }
  return [ { to: channel, message: msg.msg } ];

}
function messageListener(db, from, channel, message, messageCallback) {
  if (/^@list/.test(message))
    return listListener(db, from, channel, message, messageCallback);
}

module.exports = messageListener;
module.exports.help = [{ cmd:'@list',
                         syntax: 'Usage: @list',
                         output: {success: ['Ordered list of all list names.'],
                                  failure: []}
                       },
                       { cmd:'@list <list-name>',
                         syntax: 'Usage: @list <list-name>',
                         output: {success: ['Ordered list of all entries in <list-name>.'],
                                  failure: ['<user>: Sorry - <list-name> not found.']}
                       },
                       { cmd:'@list <list-name> add <list-item>',
                         syntax: 'Usage: @list <list-name> add <list-item>',
                         output: {success: ['<user>: OK - "<list-item>" was added to <list-name>.'],
                                  failure: []}
                       },
                       { cmd:'@list <list-name> delete <list-item>',
                         syntax: '@list <list-name> delete <list-item>',
                         output: {success: ['<user>: OK - "<list-item>" was removed from <list-name>.'],
                                  failure: ['<user>: Sorry - "<list-name>" was not found.',
                                            '<user>: Sorry - "<list-item>" was not found in <list-name>.']}
                       }];

