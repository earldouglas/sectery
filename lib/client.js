'use strict';

var irc = require('irc');

module.exports =
  new irc.Client(
    process.env.IRC_HOST,
    process.env.IRC_USER,
    {
      password: process.env.IRC_PASS,
      channels: process.env.IRC_CHANNELS.split(',')
    }
  );

