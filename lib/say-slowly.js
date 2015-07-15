'use strict';

function say_slowly(say, channel, messages) {

  var msg = messages.shift();

  if (msg) {
    say(channel, msg);
    setTimeout(function() {say_slowly(say, channel, messages);}, 500);
  }
};

module.exports = say_slowly;
