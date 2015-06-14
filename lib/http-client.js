'use strict';

var request = require('sync-request');

function mock() {
  return function(db, url) {
    if (url === 'http://stackoverflow.com/questions/11037123/%C3%A9-html-entity-code-in-title-tags') {
      return '<title>é HTML Entity code in title tags - Stack Overflow</title>';
    } else if (url === 'https://www.google.com/') {
      return '<title>Google</title>';
    } else if (url === 'http://www.simpsonquotes.com/random') {
      return JSON.stringify([{
        "_id":         "54e357bbdef4b322f97a740f",
        "show":        "The Simpsons",
        "season":      2,
        "episode":     1,
        "text":       "We have time for one more report. Bart Simpson? ",
        "timer":      "00:01:54,533 --> 00:01:58,412",
        "searchText": "We have time for one more report Bart Simpson "
      }])
    }
  };
}

function prod() {
  return function(db, url) {
    return request('GET', url).getBody().toString().replace(/\n/g, '');
  };
}

module.exports = (process.env.IRC_ENV === 'production') ? prod() : mock();
