'use strict';

var cowsay = require('cowsay');

function messageListener(db, from, channel, message, reply) {

  if (/^@cowsay/.test(message)) {
    var commandMatch = /^@cowsay\s+(.+)$/.exec(message);
    if (commandMatch) {
      var src = commandMatch[1];
      var res = cowsay.say({ text: commandMatch[1] });
      res.split("\n").forEach(function (line) {
	reply({ to: channel, message: line });
      });
    } else {
      reply({ to: channel, message: '@cowsay <text>'});
    }
  } 
}

module.exports = messageListener;
