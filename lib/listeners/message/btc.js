'use strict';

var curl = require('../../curl');

function messageListener(db, from, channel, message, reply) {
  if (/^@btc$/.exec(message)) {
    curl('http://preev.com/pulse/units:btc+usd/sources:bitstamp', function (resp) {
      var last = parseFloat(resp.btc.usd.bitstamp.last).toLocaleString('en-US', {
        style: 'currency',
        currency: 'USD',
      });
      reply({ to: channel, message: last });
    });
  }
}

module.exports = messageListener;
