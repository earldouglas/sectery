'use strict';

var httpclient = require('../../http-client');

function messageListener(db, from, channel, message) {
  if (/^@simpons */.test(message)) {
    var message;

    try {
      var quote = JSON.parse(httpclient(db, "http://www.simpsonquotes.com/random"))[0];
      message = '(S' + quote.season + 'E' + quote.episode + '): ' + quote.text;
    } catch(ex) {
      message = 'Error: ' + ex.message
    }

    return [{ to: channel, message: message }];
  }
}

module.exports = messageListener;
