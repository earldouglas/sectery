'use strict';

var util = require('../../utilities');

function getChannelList(db,channel) {
  db.list = db.list|| {};
  db.list[channel] = db.list[channel] || {};
  return db.list[channel];
}

function getListName(db,channel,listName) {
  var list = getChannelList(db,channel);
  list[listName] = list[listName] || [];
  return list[listName];
}

function findListItemIndex(list,listItem) {
  for (var i = 0; i < list.length; i++) {
    if (listItem === list[i])
      return i;
  }
  return list.length;
}

function usage(db, channel, reply){

  var message = [];

  message.push('Usage: @list <list-name>');
  message.push('Usage: @list <list-name> <add|delete> <list-item>');

  var list = getChannelList(db,channel);
  if (Object.keys(list).length > 0)
    message.push('available lists: ' + Object.keys(list).join(' '));

  reply({ to: channel, message: message });
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
  if (command == 'list') {
    var chanlist = getChannelList(db,channel);
    var list = chanlist[listName];
    if (list) {
      return {msg: list.join(', '), success: false};
    } else {
      return {msg: 
        from + ': Sorry - "'+listName+'" was not found.',
             success: false};
    }
  }

  if (command == 'add')
    return {msg: 
            from + ': OK - "'+listItem+'" was added to '+listName+'.',
            success: true};

  if (command == 'delete') {

    var channelList = getChannelList(db,channel);
    var deleteList = channelList[listName];
    if (deleteList) {
      var index = findListItemIndex(deleteList,listItem);
       if (deleteList[index]) {
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
function listListener(db, from, channel, message, reply) {

  var listMatch= /^@list\s+([_|\-,\w]+)$/.exec(message);
  var cmdMatch= /^@list\s+([_|\-,\w]+)\s+(add|delete|list)\s*(.*)$/.exec(message);
  var msg = '';

  if (listMatch) {

  } else if (cmdMatch) {
    var commands = { 'add':addListItem, 'delete':deleteListItem };
    var listName = cmdMatch[1];
    var command = cmdMatch[2];
    var listItem = cmdMatch[3];

    msg = generateMsg(db,from,channel,listName,command,listItem);
    if (msg.success) {
      commands[command](db,from,channel,listName,listItem);
    }

  } else {
    usage(db, channel, reply);
  }
  reply({ to: channel, message: msg.msg });

}
function messageListener(db, from, channel, message, reply) {
  if (/^@list/.test(message))
    listListener(db, from, channel, message, reply);
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

