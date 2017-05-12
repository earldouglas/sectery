'use strict';

var ascii_art  = require('../../ascii-art.js');
var say_slowly = require('../../say-slowly.js');

function messageListener(db, from, channel, message, reply) {
  var img_regex = /^@ascii +https?:\/\/[^ ]+\.(png|jpg|gif)$/i;

  if (img_regex.test(message)) {
    var img_url = message.replace(/^@ascii +/, '');

    reply({
      ascii_art(img_url, function(ascii) {
        reply({ to: channel, message: ascii.split("\n") });
      });
    });
  }
}

module.exports = messageListener;

module.exports.help = [{ cmd:'@ascii',
                         syntax: '@ascii <img url>',
                         output: {success: ['Multiple line ascii representation of the image.'],
                                  failure: []}
                       }];
