'use strict';

var httpclient = require('../../http-client');

var isGoodSeason = function (x) {
  return x && x >= 1 && x <= 10;
}

function messageListener(db, from, channel, message) {
  if (/^@simpsons */.test(message)) {
    var message;

    try {
      var quote;
      do {
        quote = JSON.parse(httpclient(db, "http://www.simpsonquotes.com/random"))[0];
      } while (!isGoodSeason(quote.season));
      message = '(S' + quote.season + 'E' + quote.episode + '): ' + quote.text;
    } catch(ex) {
      message = 'Error: ' + ex.message
    }

    return [{ to: channel, message: message }];
  }
}

module.exports = messageListener;
