'use strict';


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
      uri: db.contacts.config.sms.uri,
      body: 'number=' + to + '&message=' + encodeURIComponent(message)
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
  return function(db,from, to, subject, body) {
    var transporter = nodemailer.createTransport(smtpPool(db.contacts.config.email.smtpPool));
    transporter.sendMail(
      { from: from,
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
  return function() { return 'ce927a90-eda3-4d30-9f03-0d92157028bb' };
}
function uuid_prod() {
  var uuid = require('node-uuid');
  return function() {  
    return uuid.v4();
  };
}
module.exports.email = (process.env.IRC_ENV === undefined) ? email_mock() : email_prod();
module.exports.sms = (process.env.IRC_ENV === undefined) ? sms_mock() : sms_prod();
module.exports.uuid = (process.env.IRC_ENV === undefined) ? uuid_mock() : uuid_prod();
