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
  } else if (/big gulp/i.test(message)) {
    return [ { to: channel, message: 'http://biggulpshuh.com/' } ];
  } else if (/sad trombone/i.test(message)) {
    return [ { to: channel, message: 'http://sadtrombone.net/' } ];
  } else if (/dire situation/i.test(message)) {
    return [ { to: channel, message: 'http://nooooooooooooooo.com/' } ];
  } else if (/noooo/i.test(message)) {
    return [ { to: channel, message: 'http://nooooooooooooooo.com/' } ];
  } else if (/^later\.?$/i.test(message) || /^later, all\.?$/i.test(message)) {
    return [ { to: channel, message: 'Bye, ' + from + '!' } ];
  } else if (/d'?oh/i.test(message)) {
    return [ { to: channel, message: '(_8n(|)' } ];
  } else if (/salad/i.test(message)) {
    return [ { to: channel, message: "You don't win friends with salad." } ];
  }
}

module.exports = messageListener;
