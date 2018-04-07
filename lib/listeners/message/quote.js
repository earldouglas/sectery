'use strict';

var util = require('../../utilities');

function getRandomQuote(db, from, channel) {

  // random between min and max inclusive
  var getRandomInt = function(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  };

  db.quotes = db.quotes || {};
  db.quotes[channel] = db.quotes[channel] || {};
  db.quotes[channel][from] = db.quotes[channel][from] || [];

  var index = getRandomInt(0, db.quotes[channel][from].length - 1);

  return db.quotes[channel][from][index];

}

function messageListener(db, from, channel, message, reply) {
  
  if (/^@quote/.test(message)) {
    var match = /^@quote\s+([.-_|\w]+)\s*$/.exec(message);
    db.quotes = db.quotes || {};

    if (match) {
      var sayer = match[1];
      var quote = getRandomQuote(db, sayer, channel);
      if (quote) {
        reply({ to: channel,
                message: '<' + sayer + '>: ' + quote
              });
      } else {
        reply({ to: channel,
                message: from + ': Sorry, ' + util.bot() + ' has not recorded anything for ' + sayer +'.'
              });
      }
    } else {
      reply({ to: channel,
              message: 'Usage: @quote <username>'
            });
    }
  }
}

module.exports = messageListener;

module.exports.help = [{ cmd:'@quote',
                         syntax: '@quote <username>',
                         output: { success: ['<usernmae>: <random qoute>'],
                                   failure: ['<user>: Sorry, ' + util.bot() + ' has not recorded anything for <username>.']
                                 }
                       }];

