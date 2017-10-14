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

      for (var i = 0; i < lines.length; i++) {
        reply({ to: channel, message: lines[i] });
      }

    });
  }

}

module.exports = messageListener;
