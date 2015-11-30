'use strict';

var answers = [
  'It is certain.',
  'It is decidedly so.',
  'Without a doubt.',
  'Yes definitely.',
  'You may rely on it.',
  'As I see it, yes.',
  'Most likely.',
  'Outlook good.',
  'Yes.',
  'Signs point to yes.',
  'Reply hazy try again.',
  'Ask again later.',
  'Better not tell you now.',
  'Cannot predict now.',
  'Concentrate and ask again.',
  'Don\'t count on it.',
  'My reply is no.',
  'My sources say no.',
  'Outlook not so good.',
  'Very doubtful.',
];

function messageListener(db, from, channel, message) {

  var messages = [];

  var match = /^@8ball\s+(.+)$/.exec(message);
  if (match) {
    var answer = answers[Math.floor(Math.random()*answers.length)];
    messages.push({ to: channel, message: from + ': ' + answer });
  }

  return messages;
}

module.exports = messageListener;
module.exports.help = [{ cmd:'@8ball',
                         syntax: '@8ball <question>',
                         output: {success: ['1 of 20 responses.'],
                                  failure: []}
                       }];

