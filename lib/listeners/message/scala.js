'use strict';

var httpsync = require('httpsync');

function messageListener(db, from, channel, message) {

  db.scala = db.scala || {};
  db.scala.headers = db.scala.headers || {};

  if (/^@scala/.test(message)) {
    var match = /^@scala\s+(.+)$/.exec(message);
    if (match) {
      var encoded = encodeURIComponent(match[1]);
      var req = httpsync.get({
        url: 'http://www.simplyscala.com/interp?bot=irc&code=' + encoded,
        headers: db.scala.headers,
      });
      var res = req.end();
      if (res.statusCode !== 200) {
        db.scala.headers.cookie = {};
      } else if (res.headers && res.headers['set-cookie']) {
        db.scala.headers.cookie = res.headers['set-cookie'];
      }
      return [ { to: channel, message: res.data.toString().replace(/\n+$/, '') } ];
    } else {
      return [ { to: channel, message: 'Usage: @scala <expression>' } ];
    }
  }

}

module.exports = messageListener;
