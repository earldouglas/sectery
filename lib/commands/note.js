'use strict';

var nodemailer = require('nodemailer');
var smtpPool = require('nodemailer-smtp-pool');
var offline = false;

function send(from, to, subject, body) {

  if (offline) {
    return;
  }
  transporter.sendMail(
    { from: from,
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
function listener(client, config) {

  var transporter = nodemailer.createTransport(smtpPool(config.smtpPool));
  offline = config.offline;

  if (config.offline) {
    send = function () {};
  }

  return function(from, to, message) {
    if (message.length > 0) {
      if (config.contacts[from]) {
        client.say(to, 'I\'ll email you a reminder.');
        send(from, config.contacts[from], message, '');
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
module.exports.send = send;
