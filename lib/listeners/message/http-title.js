'use strict';

var cheerio = require('cheerio');
var curl = require('../../curl');

function trim(x) {
  x = x.trim().replace(/\n/g, ' ').replace(/\s\s*/g, ' ');
  if (x.length > 163) {
    x = x.substring(0, 160) + '...';
  }
  return x;
}

function messageListener(db, from, channel, message, reply) {
  var match = /(https?:\/\/[^ ]+)/.exec(message);
  if (match) {
    curl(match[1], function (html) {

      var $ = cheerio.load(html);
      var lines = [];

      if ($('title').length === 1) {
        var title = $('title').text();
        reply({ to: channel,
                message: trim(title)
              }
        );
      }

      if ($('meta[property="og:description"]').length === 1) {
        var desc = $('meta[property="og:description"]').attr("content");
        desc = trim(desc);
        reply({ to: channel, message: desc });
      }

    });
  }

}

module.exports = messageListener;
