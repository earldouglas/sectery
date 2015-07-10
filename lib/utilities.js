'use strict';

function bot_mock() {
 return 'sectery';
}

function bot_prod() {
  return process.env.IRC_USER;
}

function sms_mock() {
  return function(db,to,message) { return 0;};
}

function sms_prod() {
  var preq = require('preq');
  return function(db,to, message) {
    preq.post({
      headers: {
        'content-type': 'application/x-www-form-urlencoded'
      },
      uri: 'http://textbelt.com/text',
      body: 'number=' + to + '&message=' + encodeURIComponent(message),
    }).catch(function (e) {
      console.error('contact.sms', e);
    });
  };
}

function email_mock() {
  return function(db,from,to,subjet,body) { return 0; };
}

function email_prod() {
  var nodemailer = require('nodemailer');
  var smtpPool = require('nodemailer-smtp-pool');
  return function(db, to, subject, body) {
    var transporter = nodemailer.createTransport(
      smtpPool({
        host: "localhost",
        port: 25,
        name: "localhost",
        ignoreTLS: true,
        maxConnections: 5,
        maxMessages: 100,
        secure: false,
      })
    );
    var mail = {
      from: bot_prod() + '@irc',
      to: to,
      subject: subject,
      text: body
    };
    transporter.sendMail(
      mail,
      function (e) {
        if (e) {
          console.error('contact.email', e);
        }
      }
    );
  };
}

function uuid_mock() {
  return 'ce927a90-eda3-4d30-9f03-0d92157028bb';
}

function uuid_prod() {
  var uuid = require('node-uuid');
  return function() {
    return uuid.v4();
  };
}

function now_prod() {
  var now = new Date();
  return date_format_prod(now);
}

function now_mock() {
  return 'now of the clock';
}
function date_format_prod(d) {
  var dateFormat = require('dateformat');
  return dateFormat(d, "h:MM TT Z") + ' on ' + dateFormat(d, "ddd, mmm dS");
}

function date_format_mock(d) {
  return 'date: ' + d;
}


function regexMatch(regex,str) {
  var result;
  var tokens = [];
  while ((result = regex.exec(str)) !== null) {
    tokens.push(result[1]);
  }
  return tokens;
}
function timeout_prod(f,time) {
  setTimeout(f,time);
}
function timeout_mock(f,time) {
  setTimeout(f,10);
}

module.exports.email = (process.env.IRC_ENV === 'production') ? email_prod() : email_mock();
module.exports.sms   = (process.env.IRC_ENV === 'production') ? sms_prod()   : sms_mock();
module.exports.uuid  = (process.env.IRC_ENV === 'production') ? uuid_prod()  : uuid_mock;
module.exports.now   = (process.env.IRC_ENV === 'production') ? now_prod     : now_mock;
module.exports.date_format = (process.env.IRC_ENV === 'production') ? date_format_prod : date_format_mock;
module.exports.bot   = (process.env.IRC_ENV === 'production') ? bot_prod()   : bot_mock();
module.exports.regex_match = regexMatch;
module.exports.timeout =(process.env.IRC_ENV === 'production') ? timeout_prod : timeout_mock; 
