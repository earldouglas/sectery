'use strict';

var irc = require('irc');

var main = require('./lib/main');
var fsdb = require('./lib/fsdb');

var config = fsdb.load('config.json');
var client = new irc.Client(config.irc.host, config.irc.user, config.irc.opts);

main(client, 'config.json');
