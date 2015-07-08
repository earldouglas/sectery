'use strict';

var KryptoGame = require('../../krypto-game.js');

function messageListener(db, from, channel, message) {
  var messages = [];
  if (/^@krypto/.test(message)) { db.krypto = db.krypto || []; db.krypto[channel] = db.krypto[channel] || new KryptoGame.Krypto(); 
      if (db.krypto[channel].okToGuess())  {
        db.krypto[channel].guesser = from;
        messages.push({ to: channel, message: from +  ': OK - take a guess.'});
      } else {
        messages.push({ to: channel, message: from +  ': sorry, but ' + db.krypto[channel].guesser + ' already is guessing.'}); 
      }
  }
  if (/^@cards/.test(message)) {
      db.krypto = db.krypto || [];
      db.krypto[channel] = db.krypto[channel] || new KryptoGame.Krypto(); 
      messages.push({ to: channel, message: db.krypto[channel].hand.slice(0,5).join(', ') + ' Objective Card: ' + db.krypto[channel].hand[5]});
  }
  if (/^@guess/.test(message)) {
      db.krypto = db.krypto || [];
      db.krypto[channel] = db.krypto[channel] || new KryptoGame.Krypto(); 
      if (db.krypto[channel].guesser != from) {
        messages.push({ to: channel, message: from +  ': sorry, but it\'s ' + db.krypto[channel].guesser + '\'s turn.'});
      } else {
        var match = /^@guess\s+(.+)$/.exec(message);
        if (db.krypto[channel].checkSolution(from,match[1])) {
          messages.push({ to: channel, message: from +  ': Nice job! You got it correct!'});
          db.krypto[channel].deal();

          messages.push({ to: channel, message: db.krypto[channel].hand.slice(0,5).join(', ') + ' Objective Card: ' + db.krypto[channel].hand[5]});
        } else {
          messages.push({ to: channel, message: from +  ': Sorry, your answer is incorrect.'});
        }
        db.krypto[channel].guesser = null;
      }
  }
  return messages;
}

module.exports = messageListener;
