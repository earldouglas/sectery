'use strict';

function messageListener(db, from, channel, message, reply) {

  db.nicks = db.nicks || {};
  db.nicks[channel] = db.nicks[channel] || {};

  db.points = db.points || {};
  db.points[channel] = db.points[channel] || {};

  var addPoints = function(nick, points) {
    db.points[channel][nick] = db.points[channel][nick] || 0; 
    db.points[channel][nick] = Math.max(0, db.points[channel][nick] + points);
    return db.points[channel][nick];
  }

  var showPoints = function(points) {
    return [ (points === 0) ? 'no' : points
           , (points === 1) ? 'point' : 'points'
           ].join(' ');
  }

  var splits = message.split(' ');
  for (var i = 0; i < splits.length; i++) {
    var split = splits[i];
    var nick = split.substring(0, split.length - 2);
    if (db.nicks[channel][nick]) {
      var delta = 0;
      if (/.+\+\+/.test(split)) {
        delta = 1;
      } else if (/.+--/.test(split)) {
        delta = -1;
      }
      if (delta !== 0) {
        if (nick === from) {
          reply({ to: channel
                        , message: nick + ': You can\'t change your own points.'
                        });
        } else {
          reply({ to: channel
                        , message: nick + ' has ' + showPoints(addPoints(nick, delta)) + '.'
                        });
        }
      }
    }
  }

}

module.exports = messageListener;
