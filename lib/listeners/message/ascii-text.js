'use strict';

var figlet     = require('figlet');
var say_slowly = require('../../say-slowly.js');

var random_font = function() {
  var fonts = [
    'Basic',
    'Big',
    'JS Stick Letters',
    'Kban',
    'Slant',
    'Soft'
  ];

  return fonts[Math.floor(Math.random() * fonts.length)];
}

if (process.env.IRC_ENV != 'production') {
  random_font = function() { return 'Soft' };
}

function messageListener(db, from, channel, message) {
  var img_regex   = /^@ascii +https?:\/\//;
  var ascii_regex = /^@ascii +[^ ]/

  if (ascii_regex.test(message) && !img_regex.test(message)) {
    var text  = message.replace(/^@ascii +/, '');
    var ascii = figlet.textSync(text, {
      font:             random_font(),
      horizontalLayout: 'default',
      verticalLayout:   'default'
    });

    return [{
      callback: function(say) {
        say_slowly(say, channel, ascii.split("\n"));
      }
    }];
  }
}

module.exports = messageListener;

module.exports.help = [{ cmd:'@ascii',
                         syntax: '@ascii <text>',
                         output: {success: ['Multiple line ascii representation of the text.'],
                                  failure: []}
                       }];
