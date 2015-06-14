'use strict';

var ascii_art  = require('../../ascii-art.js');
var say_slowly = require('../../say-slowly.js');

function messageListener(db, from, channel, message) {
  var img_regex = /^@ascii +https?:\/\/[^ ]+\.(png|jpg|gif)$/i;

  if (img_regex.test(message)) {
    var img_url = message.replace(/^@ascii +/, '');

    return [{
      callback: function(say) {
        ascii_art(img_url, function(ascii) {
          say_slowly(say, channel, ascii.split("\n"));
        });
      }
    }];
  }
}

module.exports = messageListener;
