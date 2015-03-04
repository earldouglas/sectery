'use strict';

function messageListener(db, from, channel, message) {
  if (/table\s*flip/.test(message) || /flip\s*table/.test(message)) {
    return [ { to: channel, message: '╯°□°）╯︵ ┻━┻' } ];
  } else if (/shrug/.test(message)) {
    return [ { to: channel, message: '¯\_(ツ)_/¯' } ];
  }
}

module.exports = messageListener;
