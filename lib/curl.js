'use strict';

var request = require('sync-request');

module.exports =
  function(db, url) {
    var options = {
      headers : {'Content-type':'text/plain','user-agent':'curl/7.35.0'}
    };
    return request('GET',url, options).getBody('UTF8').split('\n');
  };

