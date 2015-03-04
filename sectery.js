'use strict';

var irc     = require('irc');
var sectery = require('./lib/sectery');

var client;

[ 'IRC_HOST', 'IRC_USER', 'IRC_PASS', ].forEach(function (x) {
  if (process.env[x] === undefined) {
      console.log('Please set ' + x + ' and try again.');                      
      process.exit();                                                                
  }  
});

var client = new irc.Client(
  process.env.IRC_HOST,
  process.env.IRC_USER,
  { password: process.env.IRC_PASS }
);

sectery(client);
