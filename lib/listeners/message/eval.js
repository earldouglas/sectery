'use strict';

var url = require('url');
var curl = require('../../curl');

function messageListener(db, from, channel, message, reply) {

  if (/^@eval/.test(message)) {
    var commandMatch = /^@eval\s+(.+)$/.exec(message);
    if (commandMatch) {
      var src = commandMatch[1];
      var res = curl('https://earldouglas.com/api/haskeval?src=' + encodeURIComponent(src), function (res) {
        var message = res.trim();
        if (message.length > 0) {
          reply({ to: channel, message: message});
        }
      });
    } else {
      reply({ to: channel, message: '@eval <location>'});
    }
  } 

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
