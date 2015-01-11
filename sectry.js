'use strict';

var irc = require('irc');
var fsdb = require('./lib/fsdb');
var main = require('./lib/main');

var config = fsdb.load('config.json');
var client = new irc.Client(config.irc.host, config.irc.user, config.irc.opts);

main(client, 'config.json');
