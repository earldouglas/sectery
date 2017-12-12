'use strict';

var cheerio = require('cheerio');
var curl = require('../../curl');

function messageListener(db, from, channel, message, reply) {
  var match = /(https?:\/\/[^ ]+)/.exec(message);
  if (match) {
    curl(match[1], function (res) {

      var $ = cheerio.load(res);
      var lines = [];

      if ($('meta[property="og:description"]').length === 1) {
        var desc = $('meta[property="og:description"]').attr("content");
        lines = desc.trim().split('\n');
      } else if ($('title').length === 1) {
        var title = $('title').text();
        lines = title.trim().split('\n');
      }

      var res = lines.join(' ');
      if (res.length > 83) {
        res = res.substring(0, 80) + '...';
      }
      reply({ to: channel, message: res });

    });
  }

}

module.exports = messageListener;
