'use strict';

function mock() {
  return function(db, src) {
    if (src === '2 + 3') {
      return 'res0: Int = 5';
    } else {
      return 'res1: Int = 12';
    }
  };
}

function prod() {
  var request = require('sync-request');
  return function(db, src) {
    var encoded = encodeURIComponent(src);
    var res = request(
      'GET',
      'http://www.simplyscala.com/interp?bot=irc&code=' + encoded,
      { headers: db.scala.headers, }
    );
    if (res.statusCode !== 200) {
      db.scala.headers.cookie = {};
    } else if (res.headers && res.headers['set-cookie']) {
      db.scala.headers.cookie = res.headers['set-cookie'];
    }
    return res.getBody().toString().replace(/\n+$/, '');
  };
}

module.exports = (process.env.IRC_PROD === 'true') ? prod() : mock();
