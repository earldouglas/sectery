'use strict';

function messageListener(db, from, channel, message) {
  if (/table\s*flip/i.test(message) || /flip\s*table/.test(message)) {
    return [ { to: channel, message: '╯°□°）╯︵ ┻━┻' } ];
  } else if (/shrug/i.test(message)) {
    return [ { to: channel, message: '¯\\_(ツ)_/¯' } ];
  }
}

module.exports = messageListener;
