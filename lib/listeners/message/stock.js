'use strict';

var httpclient = require('../../http-client');
var parse      = require('csv-parse/lib/sync');


function messageListener(db, from, channel, message) {
  var match = /@stock\s+([A-Z][A-Z]?[A-Z]?[A-Z]?)/.exec(message)
  if (match) {
    var url = 'http://download.finance.yahoo.com/d/quotes.csv?s=' +
              match[1] + '&f=nsl1';
    var res = httpclient(db, url, true);
    var rows = parse(res, {});

    var messages = [];
    for (var i = 0; i < rows.length; i++) {
      var row = rows[0];
      messages.push({
        to: channel,
        message: row[0] + ' (' + row[1] + '): $' + row[2]
      });
    }
    return messages;
  }
}

module.exports = messageListener;
