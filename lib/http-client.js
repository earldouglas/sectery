'use strict';

var request = require('sync-request');

function messageListener(db, from, channel, message) {

  db.scala = db.scala || {};
  db.scala.headers = db.scala.headers || {};

  if (/^https?:\/\/[^ ]+$/.test(message)) {
    var res = request('GET', message);
    var match = /<title>(.+)<\/title>/.exec(res.getBody().toString().replace(/\n/g, ''));
    if (match) {
      var decoded = match[1].replace(/&#\d+;/gm,function(s) {
        return String.fromCharCode(s.match(/\d+/gm)[0]);
      });
      return [ { to: channel, message: decoded } ] ;
    }
  }
}

function mock() {
  return function(db, url) {
    if (url === 'http://stackoverflow.com/questions/11037123/%C3%A9-html-entity-code-in-title-tags') {
      return '<title>é HTML Entity code in title tags - Stack Overflow</title>';
    } else if (url === 'https://www.google.com/') {
      return '<title>Google</title>';
    }
  };
}

function prod() {
  return function(db, url) {
    return request('GET', url).getBody().toString().replace(/\n/g, '');
  };
}

module.exports = (process.env.IRC_PROD === 'true') ? prod() : mock();
