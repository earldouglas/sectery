'use strict';

var request = require('sync-request');

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

module.exports = (process.env.IRC_ENV === undefined) ? mock() : prod();
