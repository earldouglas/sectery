'use strict';

var request = require('sync-request');

module.exports =
  function(db, src) {
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

