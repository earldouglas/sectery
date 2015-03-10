'use strict';

var nodemailer = require('nodemailer');
var smtpPool = require('nodemailer-smtp-pool');
var preq = require('preq');

function sms(db,to, message) {
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
function email(db,from, to, subject, body) {
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
}

module.exports.email = email;
module.exports.sms = sms;
