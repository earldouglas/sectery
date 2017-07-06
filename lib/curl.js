'use strict';

var preq = require('preq');

module.exports =
  function(url, k) {
    preq.get({ headers: { "user-agent": "curl" }, uri: url }).then(function (res) { k(res.body); });
  };
