'use strict';

var fs = require('fs');
var say_slowly = require('../../say-slowly.js');

Array.prototype.flatMap = function(lambda) { 
      return Array.prototype.concat.apply([], this.map(lambda)); 
};
function loadHelp() {
  var help = fs.readdirSync(__dirname +'/../').flatMap(function(eventName) {
    var filepath = __dirname + '/../' + eventName;

    // all js files that have exported 'help'
    var files = fs.readdirSync(filepath).filter(
                                                function(file) { 
                                                  return /^[^\.].*.js$/.test(file) && 
                                                         require(filepath + '/' + file).help;
                                                }); 
    return files.flatMap(
                         function(file) {
                           var f = require(filepath + '/' + file);
                           return f.help;
                         });
  }).filter(function(a) { return a.length !== 0;});
  return help;
}
function messageListener(db, from, channel, message) {

  if (/^@help$/.test(message)) {
    var help = loadHelp();
    return [ { to: channel, 
               message: 'Available commands: ' + help.map(function(h) {  return h.cmd; }).join(', ')}];
  }
  if (/^@help\s+(.+)$/.test(message)) {

    var match = /^@help\s+(.+)$/.exec(message);
    var help = loadHelp();
    help = help.filter(function (h) { return h.cmd == match[1]; });
    if (help.length === 0) {
      return [ { to: channel, message: from + ': Command \'' + match[1] + '\' not found.'}];
    }
    help = help[0];
    var result = [];

    result.push({ to: channel, message:  'Syntax: ' + help.syntax });
    result.push({
      callback: function(say) {
        var response = ['On Success: '].concat(help.output.success);
        var response2 = ['On Failure: '].concat(help.output.failure);
        response = response.concat(response2);
        say_slowly(say, channel, response);
      }
    });
    return result;
  }
}

module.exports = messageListener;
