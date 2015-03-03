'use strict';

var uuid = require('node-uuid');
var note = require('./note.js');
var sms  = require('./sms.js');

function usage(client, channel, extraInfo){
  var message = '';
  if (extraInfo !== '') {
    message = 'Error: ' + extraInfo + ', ';
  }
  client.say(channel, message + 'Usage: @setup <note|sms> <email@email.com|phone|code>');
}
function isCode(code) {
  var match = /^\w{8}-\w{4}-\w{4}-\w{4}-\w{12}$/.exec(code);
  if (match) {
    return true;
  } else {
    return false;
  }
}
function updateConfig(config,channel,from,command,verificationCode,argument) {
  config[channel] = config[channel] || {};
  config[channel][from] = config[channel][from] || {};
  config[channel][from][command] = config[channel][from][command] || {};
  config[channel][from][command].code = verificationCode;
  config[channel][from][command].info = argument;
  config.set(channel,from + ':' + command + ':code',verificationCode);
  config.set(channel,from + ':' + command + ':info',argument);
}
function listener(client, config) {

  return function(from, channel, message) {

    var match = /^(\w+)\s*(\S+@\S+|\d{3}-?\d{3}-?\d{4}|\w{8}-\w{4}-\w{4}-\w{4}-\w{12})\s*(.*)$/.exec(message);

    var command = match && match[1] || '';
    var argument = match && match[2] || '';
    //matches first word, then either an 'email' (non-blank characters surrounding @), or a UUID
    var verificationCode = uuid.v4();
    var verificationMessage = 'This is your verification code: "' + verificationCode + '". Run the following command in ' + channel + ':\n @setup '+ command + ' ' + verificationCode;

    var validateCode = function() {
      var sentCode = config[channel][from][command].code;
      var contactInfo = config[channel][from][command].info;
      console.log(sentCode, argument);
      if (argument === sentCode) {
        config.getConfig(command).contacts[from]  = config.getConfig(command).contacts[from] || {};
        config.getConfig(command).contacts[from] = contactInfo;
        console.log('code valid');
        client.say(channel, from + ', code validated.');
       // config.clear(channel,from + ':' + argument); 
      } else {
        console.log('code invalid');
        client.say(channel, from + ', code invalid. Please try again.');
      }
    };
    var validateSms = function () {
      if (isCode(argument)) {
        validateCode();
      } else {
        updateConfig(config,channel,from,command,verificationCode,argument);
        sms.sms(argument, verificationMessage);
        client.say(channel, from + ', validation code sent! Check your texts.');
      }
    };
    var validateEmail = function () {
      if (isCode(argument)) {
        validateCode();
      } else {
        updateConfig(config,channel,from,command,verificationCode,argument);
        note.send(from,
                  argument,
                  channel + 'email verification',
                  verificationMessage);
        client.say(channel, from + ', validation code sent! Check your email.');
      }
    };

    var commands = {'note':validateEmail, 'sms':validateSms};
    if (match && commands[command]) {
      commands[command](command,argument);
    } else {
      usage(client,channel,'');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;

