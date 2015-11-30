'use strict';

var answers = [
  'Yes.',
  'Yep.',
  'Yeah.',
  'Obviously.',
  'Clearly.',
  'Indubitably.',
  'Hell to the yes.',
  'Of course.',
  'Does a bear crap in the woods?',
];

function messageListener(db, from, channel, message) {

  var messages = [];

  var match = /^@9ball\s+(.+)$/.exec(message);
  if (match) {
    var answer = answers[Math.floor(Math.random()*answers.length)];
    messages.push({ to: channel, message: from + ': ' + answer });
  }

  return messages;
}

module.exports = messageListener;

module.exports.help = [{ cmd:'@9ball',
                         syntax: '@9ball <question>',
                         output: {success: ['Always.']
                                  failure: []}
                       }];
