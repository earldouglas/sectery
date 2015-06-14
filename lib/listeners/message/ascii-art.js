'use strict';

var ascii_art = require('../../ascii-art.js');

function messageListener(db, from, channel, message) {
  var img_regex = /^https?:\/\/[^ ]+\.(png|jpg|gif)$/i;

  if (img_regex.test(message)) {
    var img_url = message;

    return [{
      callback: function(say) {
        ascii_art(img_url, function(art) {
          say(channel, art);
        });
      }
    }];
  }
}

module.exports = messageListener;
