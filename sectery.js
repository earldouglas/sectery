'use strict';

var irc = require('irc');

var main = require('./lib/main');
var nconf = require('nconf');

nconf.file('config.json');
var client = new irc.Client(
  nconf.get('irc:host'),
  nconf.get('irc:user'),
  nconf.get('irc:opts'));

main(client, 'config.json');
