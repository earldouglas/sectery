'use strict';

var cheerio = require('cheerio');
var curl = require('../../curl');

function messageListener(db, from, channel, message, reply) {
  var match = /(https?:\/\/[^ ]+)/.exec(message);
  if (match) {
    curl(match[1], function (html) {

      var $ = cheerio.load(html);
      var lines = [];

      if ($('title').length === 1) {
        var title = $('title').text();
        reply({ to: channel, message: title.trim() });
      }

      if ($('meta[property="og:description"]').length === 1) {
        var desc = $('meta[property="og:description"]').attr("content");
        desc = desc.trim();
        if (desc.length > 163) {
          desc = desc.substring(0, 160) + '...';
        }
        reply({ to: channel, message: desc });
      }

    });
  }

}

module.exports = messageListener;
