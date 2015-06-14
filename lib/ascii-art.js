'use strict';

var ascii = require('image-to-ascii');

function mock() {
  return function(url, callback) {
    callback('[ascii art]');
  };
}

function prod() {
  return function(url, callback) {
    ascii({
      colored: false,
      path:    url,
      size:    {height: 30}
    }, function (err, result) {
      callback(err ? 'Ascii art error: ' + err : result);
    });
  };
}

module.exports = (process.env.IRC_ENV === undefined) ? mock() : prod();
