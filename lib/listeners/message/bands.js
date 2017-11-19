'use strict';

var cheerio = require('cheerio');
var curl = require('../../curl');

function extractBQI(url) {
  // url should match http://www.bandcondx.com/21.jpg
  return url.replace(/http:\/\/www\.bandcondx\.com\/(\d\d*)\.jpg$/,
                     function(match, p1, offset, string) { return p1; });
}

function messageListener(db, from, channel, message, reply) {
  if ('@bands' === message) {
    curl('http://75.35.171.117/', function (res) {

      var $ = cheerio.load(res);

      var bqis = {
        '40': extractBQI($('IMG').toArray()[2].attribs.src)
      };

      reply({ to: channel, message: '40m BQI: ' + bqis['40'] });

    });
  }

}

module.exports = messageListener;
