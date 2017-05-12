'use strict';

var curl  = require('../../curl');
var parse = require('csv-parse/lib/sync');

function messageListener(db, from, channel, message, reply) {
  var match = /@stock\s+([A-Z]+)/.exec(message)
  if (match) {
    var url = 'http://download.finance.yahoo.com/d/quotes.csv?s=' +
              match[1] + '&f=nsl1';
    curl('http://download.finance.yahoo.com/d/quotes.csv?s=' + match[1] + '&f=nsl1', function (body) {
      var rows = parse(res, {});
      for (var i = 0; i < rows.length; i++) {
        var row = rows[0];
        reply({
          to: channel,
          message: row[0] + ' (' + row[1] + '): $' + row[2]
        });
      }
    });
  }
}

module.exports = messageListener;
