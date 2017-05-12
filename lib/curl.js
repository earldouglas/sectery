'use strict';

var preq = require('preq');

module.exports =
  function(url, k) {
    preq.get({ uri: url }).then(function (res) { k(res.body); });
  };
