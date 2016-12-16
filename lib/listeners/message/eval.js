'use strict';

var curl = require('../../curl');

function messageListener(db, from, channel, message) {

  var messages = [];

  if (/^@eval/.test(message)) {
    var commandMatch = /^@eval\s+(.+)$/.exec(message);
    if (commandMatch) {
      var src = commandMatch[1];
      var url = 'https://earldouglas.com/projects/haskeval/?src=' +
                encodeURIComponent(src);
      var res = curl(db, url);
      for (var x = 0; x < res.length; x++) {
        var message = res[x];
        if (message && message.trim().length > 0) {
          messages.push({ to: channel, message: message});
        }
      }
    } else {
      messages.push({ to: channel, message: '@eval <location>'});
    }
  } 
  return messages;

}

module.exports = messageListener;
module.exports.help = [
  { cmd:'@eval',
    syntax: '@eval <expression>',
    output: {
      success: ['result of evaluating <expression>'],
      failure: []
    }
  }
];
