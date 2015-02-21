'use strict';

var nodemailer = require('nodemailer');
var smtpPool = require('nodemailer-smtp-pool');

function listener(client, config) {

  var transporter = nodemailer.createTransport(smtpPool(config.smtpPool));
  var send = function (to, subject, body) {
    transporter.sendMail(
      { from: config.from,
        to: to,
        subject: subject,
        text: body
      },
      function (e) {  
        if (e) {
          console.error('@note', e);
        }
      }
    );
  };

  if (config.offline) {
    send = function () {};
  }

  return function(from, to, message) {
    if (message.length > 0) {
      if (config.contacts[from]) {
        client.say(to, 'I\'ll email you a reminder.');
        send(config.contacts[from], message, '');
      } else {
        client.say(to, 'I don\'t know your email address.');
      }
    } else {
      client.say(to, 'Usage: @note <message>');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
