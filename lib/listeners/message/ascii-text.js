'use strict';

var figlet = require('figlet');
var util = require('../../utilities');

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
};

if (process.env.IRC_ENV != 'production') {
  random_font = function() { return 'Soft'; };
}

function messageListener(db, from, channel, message, reply) {
  var img_regex   = /^@ascii +https?:\/\//;
  var ascii_regex = /^@ascii +[^ ]/;

  if (ascii_regex.test(message) && !img_regex.test(message)) {
    var cmd = message.match(/^@ascii +(blink)? +([^ ].*)$/);
    var formatLine = cmd[1] ? util.blink_text : function(s) { return s; };
    var text = cmd[2];
    var ascii = figlet.textSync(text, {
      font:             random_font(),
      horizontalLayout: 'default',
      verticalLayout:   'default'
    });

    ascii.split("\n").forEach(function (line) {
      reply({ to: channel, message: formatLine(line) });
    });
  }
}

module.exports = messageListener;

module.exports.help = [{ cmd:'@ascii',
                         syntax: '@ascii [blink] <text>',
                         output: {success: ['Multiple line ascii representation of the text.'],
                                  failure: []}
                       }];
