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
function listener(client, config) {

  return function(from, channel, message) {

    var match = /^(\w+)\s*(\S+@\S+|\d{3}-?\d{3}-?\d{4}|\w{8}-\w{4}-\w{4}-\w{4}-\w{12})\s*(.*)$/.exec(message);

    var command = match && match[1] || '';
    var argument = match && match[2] || '';
    //matches first word, then either an 'email' (non-blank characters surrounding @), or a UUID
    var verificationCode = uuid.v4();
    var verificationMessage = 'This is your verification code: "' + verificationCode + '". Run the following command in ' + channel + ':\n @setup '+ command + ' ' + verificationCode;

    var validateCode = function() {
      var sentCode = config[channel][from][command];
      var contactInfo = config[channel][from]['info'];
      console.log(sentCode,contactInfo);
      if (argument === sentCode) {
        console.log('code validated');
        client.say(channel, from + ', code validated.');
        console.log(command,config.getConfig(command));
        config.getConfig(command)['contacts'][from] = contactInfo;
        config.clear(channel,from + ':' + argument); 

      } else {
        client.say(channel, from + ', code invalid. Please try again.');
      }
    };
    var validateSms = function () {
      if (isCode(argument)) {
        validateCode();
      } else {
        console.log(config);
        config[channel][from][command]['code'] = verificationCode;
        config[channel][from][command]['info'] = verificationCode;
        config.set(channel,from + ':' + command + ':code',verificationCode);
        config.set(channel,from + ':' + command + ':info',argument);
        console.log(config);
        sms.sms(argument, verificationMessage);
        client.say(channel, from + ', validation code sent! Check your texts.');
      }
    };
    var validateEmail = function () {
      if (isCode(argument)) {
        validateCode();
      } else {
        console.log(config);
        config[channel][from][command]['code'] = verificationCode;
        config[channel][from][command]['info'] = verificationCode;
        config.set(channel,from + ':' + command + ':code',verificationCode);
        config.set(channel,from + ':' + command + ':info',argument);
        console.log(config);
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

