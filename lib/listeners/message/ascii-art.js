'use strict';

var ascii_art = require('../../ascii-art.js');

function messageListener(db, from, channel, message) {
  var img_regex = /^@ascii +https?:\/\/[^ ]+\.(png|jpg|gif)$/i;

  if (img_regex.test(message)) {
    var img_url = message.replace(/^@ascii +/, '');

    return [{
      callback: function(say) {
        ascii_art(img_url, function(ascii) {
          var lines = ascii.split("\n");

          slowly_say(say, channel, lines);
        });
      }
    }];
  }
}

function slowly_say(say, channel, lines) {
  var line = lines.shift();

  if (line) {
    say(channel, line);
    setTimeout(function() {slowly_say(say, channel, lines);}, 1000);
  }
}

module.exports = messageListener;
