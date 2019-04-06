'use strict';

var fs = require('fs');

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
function messageListener(db, from, channel, message, reply) {

  var help = loadHelp();

  if (/^@help$/.test(message)) {
    return [ { to: channel, 
               message: 'Available commands: ' + help.map(function(h) {  return h.cmd; }).join(', ')}];
  }
  if (/^@help\s+(.+)$/.test(message)) {

    var match = /^@help\s+(.+)$/.exec(message);
    help = help.filter(function (h) { return h.cmd == match[1]; });
    if (help.length === 0) {
      return [ { to: channel, message: from + ': Command \'' + match[1] + '\' not found.'}];
    }
    help = help[0];
    var result = [];

    reply({ to: channel, message:  'Syntax: ' + help.syntax });

    var response = ['On Success: '].concat(help.output.success);
    var response2 = ['On Failure: '].concat(help.output.failure);
    response = response.concat(response2);
    reply({ to: channel, message: response });
  }
}

module.exports = messageListener;
module.exports.help = [{ cmd:'@help',
                         syntax: '@help, @help <@command>',
                         output: {success: ['Syntax: <command syntax>',
                                            'On Success: <sucess output>',
                                            'On Failure: <failure output>'],
                                  failure: ['<user>: Command \'<command>\' not found.']}
                       }];

