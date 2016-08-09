'use strict';

var httpclient = require('../../http-client');

function messageListener(db, from, channel, message) {
  if (/@zestimate/.test(message)) {
    var match = /^@zestimate\s+(.*)$/.exec(message)
    if (match) {
  
      var addr = encodeURIComponent(match[1].replace(/[\s,]+/g, '-'));
      var url = 'http://www.zillow.com/homes/' + addr + '_rb/';
  
      var res = httpclient(db, url);
      var match2 = /<div class="zest-value">([$]\d+,\d+)<\/div>/.exec(res);
      if (match2) {
        return [ { to: channel, message: match2[1] } ] ;
      }
  
    } else {
      return [ { to: channel, message: 'Usage: @zestimate <123 Foo St, City ST 12345>' } ] ;
    }
  }
}

module.exports = messageListener;
