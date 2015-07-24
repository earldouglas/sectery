'use strict';

var request = require('sync-request');

module.exports =
  function(db, url) {
    return request('GET', url).getBody().toString().replace(/\n/g, '');
  };

