'use strict';
var uuid = require('node-uuid');
var contact = require('../../contact.js');

function usage(extraInfo){
  var message = '';
  if (extraInfo !== '') {
    message = 'Error: ' + extraInfo + '. ';
  }
  return message + 'Usage: @setup <email|sms> <email@email.com|phone|code>';
}
function isCode(code) {
  var match = /^\w{8}-\w{4}-\w{4}-\w{4}-\w{12}$/.exec(code);
  if (match) {
    return true;
  } else {
    return false;
  }
}
function updateDbSetup(db,from,command,verificationCode,argument) {
  db.setup = db.setup || {};
  db.setup[from] = db.setup[from] || {};
  db.setup[from][command] = db.setup[from][command] || {};
  db.setup[from][command].code = verificationCode;
  db.setup[from][command].info = argument;
}
function updateDbContactInfo(db,from,command,contactInfo) {
  db.contactinfo = db.contactinfo || {}; 
  db.contactinfo[from] = db.contactinfo[from] || {};
  db.contactinfo[from][command] = contactInfo;
}

function pmListener(db, from, message) {
  var match   = /^@setup\s*(\w+)\s*(\S+@\S+|\d{3}-?\d{3}-?\d{4}|\w{8}-\w{4}-\w{4}-\w{4}-\w{12})\s*(.*)$/.exec(message);
  var command = match && match[1] || '';
  var argument = match && match[2] || '';
  console.log(db,from,message,command, argument);

  //matches first word, then either an 'email' (non-blank characters surrounding @), or a UUID
  var verificationCode = uuid.v4();
  var verificationMessage = 'This is your verification code: "' + verificationCode + '". Run the following command:\n/msg sectary @setup '+ command + ' ' + verificationCode;

  var validateCode = function() {
    var sentCode = db.setup[from][command].code || '';
    var contactInfo = db.setup[from][command].info || '';

    if (argument === sentCode) {
      console.log('code valid');
      updateDbContactInfo(db,from,commant,contactInfo);
      delete db.setup[from];
      return from +', code validated.';
    } else {
      console.log('code invalid');
      return from + ', code invalid. Please try again.';
    }
  };
  var validateSms = function () {
    if (isCode(argument)) {
      return validateCode();
    } else {
      updateDbSetup(db,from,command,verificationCode,contact);
      contact.sms(db,argument, verificationMessage);
      return from + ', validation code sent! Check your texts.';
    }
  };
  var validateEmail = function () {
    if (isCode(argument)) {
      validateCode();
    } else {
      updateDbSetup(db,from,command,verificationCode,argument);
      contact.email(db,
                    from,
                argument,
                'email verification',
                verificationMessage);
      return from + ', validation code sent! Check your texts.';
    }
  };

  var commands = {'email':validateEmail, 'sms':validateSms};
  if (/^@setup.*$/.exec(message)) {
    var msg = usage('');
    if (match && commands[command]) {
      msg = commands[command](command,argument);
    } 
    return [ { to: from, message: msg } ];
  }
}

module.exports = pmListener;
