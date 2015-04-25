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
    transporter.sendMail(
      { from: bot_prod(),
        to: to,
        subject: subject,
        text: body
      },
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
  var dateFormat = require('dateformat');
  var now = new Date();
  return dateFormat(now, "h:MM TT Z") + ' on ' + dateFormat(now, "ddd, mmm dS");
}

function now_mock() {
  return 'now of the clock';
}

module.exports.email = (process.env.IRC_ENV === 'production') ? email_prod() : email_mock();
module.exports.sms   = (process.env.IRC_ENV === 'production') ? sms_prod()   : sms_mock();
module.exports.uuid  = (process.env.IRC_ENV === 'production') ? uuid_prod()  : uuid_mock;
module.exports.now   = (process.env.IRC_ENV === 'production') ? now_prod     : now_mock;
module.exports.bot   = (process.env.IRC_ENV === 'production') ? bot_prod()   : bot_mock();
