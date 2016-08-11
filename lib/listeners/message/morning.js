'use strict';

var replies = [ "Yoho!"
              , "Morning!"
              , "Buenos ding-dong-diddly-dias, señor!"
              , "Good 'morrow!"
              , "Ante meridiem!"
              ];
              
function messageListener(db, from, channel, message) {
  var match = /morning/i.exec(message);
  var messages = [];
  if (match) {
    var reply = replies[Math.floor(Math.random() * (replies.length + 1))];
    messages.push({ to: channel, message: reply });
  }
  return messages;
}

module.exports = messageListener;
