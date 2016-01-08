'use strict';

function messageListener(db, from, channel, message) {
  if (/table\s*flip/i.test(message) || /flip\s*table/i.test(message)) {
    return [ { to: channel, message: '╯°□°）╯︵ ┻━┻' } ];
  } else if (/table\s*fix/i.test(message) || /fix\s*table/i.test(message)) {
    return [ { to: channel, message: '┬──┬ ノ( ゜-゜ノ)' } ];
  } else if (/shrug/i.test(message)) {
    return [ { to: channel, message: '¯\\_(ツ)_/¯' } ];
  } else if (/^That's hilarious.$/i.test(message)) {
    return [ { to: channel, message: 'Zing!' } ];
  } else if (/╯°□°）╯︵ ┻━┻/.test(message)) {
    return [ { to: channel, message: '┬──┬ ノ( ゜-゜ノ)' } ];
  } else if (/la revoluci[oó]n/i.test(message)) {
    return [ { to: channel, message: '¡Viva!' } ];
  } else if (/the joke/i.test(message)) {
    return [ { to: channel, message: 'http://thatsthejoke.net/' } ];
  } else if (/coming up milhouse/i.test(message)) {
    return [ { to: channel, message: 'http://comingupmilhouse.com/' } ];
  } else if (/big gulps/i.test(message)) {
    return [ { to: channel, message: 'http://biggulpshuh.com/' } ];
  }
}

module.exports = messageListener;
