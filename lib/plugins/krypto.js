'use strict';

var preq = require('preq');

var solutions = {};

function solve(message, channel) {
  delete solutions[channel];
  var match = /^\D*(\d+)\D*(\d+)\D*(\d+)\D*(\d+)\D*(\d+)\D*(\d+)\D*$/.exec(message);
  if (match) {
    preq.get({
      uri: 'http://sortelli.com/cgi-bin/krypto_solver_js?' +
           match[1] + ',' + match[2] + ',' + match[3] + ',' +
           match[4] + ',' + match[5] + ',' + match[6]
    }).then(function (res) {
      var lines = res.body.split('\n');
      if (lines && lines.length > 1) {
        var first = lines[1].replace(/^answers.push\("/, '').replace(/ = \d+"\);$/, '').split(' ');
        solutions[channel] = toinfix(first);
      }
    });
  }
}

function toinfix(rpn) {
  var infix = [];
  rpn.forEach(function (x) {
    if (parseInt(x)) {
      infix = [x].concat(infix);
    } else {
      var operands = infix.splice(0,2);
      var expr = '(' + operands[1] + ' ' + x + ' ' + operands[0] + ')';
      infix = [expr].concat(infix);
    }
  });
  return infix.join(' ');
}

function listener(client) {
  return function(from, to, message) {
    if (/^Cards:/.test(message)) {
      solve(message, to);
    } else if (/^@krypto$/.test(message)) {
      if (solutions[to]) {
        client.say(to, '::krypto');
        setTimeout(function () {
          client.say(to, '::guess ' + solutions[to]);
          delete solutions[to];
        }, 1000);
      } else {
          client.say(to, 'no solution found');
      }
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
