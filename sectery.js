var sectery = require('./lib/sectery');
var client = require('./lib/client');

[ 'IRC_ENV', 'IRC_HOST', 'IRC_USER', 'IRC_PASS', 'IRC_CHANNELS', ].forEach(
  function (x) {
    if (process.env[x] === undefined) {
        console.log('Please set ' + x + ' and try again.');
        process.exit();
    }
  }
);

sectery(client);
