'use strict';

var sectery = require('./lib/sectery');

[ 'IRC_PROD', 'IRC_HOST', 'IRC_USER', 'IRC_PASS', 'IRC_CHANNELS', ].forEach(
  function (x) {
    if (process.env[x] === undefined) {
        console.log('Please set ' + x + ' and try again.');                      
        process.exit();                                                                
    }  
  }
);

var client = sectery(require('./lib/client'));
sectery(client);
