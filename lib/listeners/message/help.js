'use strict';

var fs = require('fs');

function messageListener(db, from, channel, message) {
  if (/^@help$/.test(message)) {
    console.log(message);
    var a = fs.readdirSync(__dirname +'/../').map(function(eventName) {
      return fs.readdirSync(__dirname + '/../' + eventName).map(function(file) {
        return file.substring(0,file.lastIndexOf('.'));
      })
    });  
 
    return [ { to: channel, message: 'available commands: ' + a.join(' ')} ];
  }
}

module.exports = messageListener;
