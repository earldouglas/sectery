'use strict';

var ascii = require('image-to-ascii');

module.exports =
  function(url, callback) {
    ascii({
      colored: false,
      path:    url,
      size:    {height: 30}
    }, function (err, result) {
      callback(err ? 'Ascii art error: ' + err : result);
    });
  };
