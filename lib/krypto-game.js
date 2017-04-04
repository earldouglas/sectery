'use strict';

var util = require('./utilities.js');

function Krypto() {
  var self = this;
  self.guesser = null; 
  self.hand = self.deal();
};

Krypto.prototype.hand = this.hand;

Krypto.prototype.deal = function() {
  function deck() {
    function add(x, count) {
      for (var j = 0; j < count; j++) cards.push(x);
    }
    var cards = [];
    for (var i =  1; i <=  6; i++) { add(i, 3); }
    for (i =  7; i <= 10; i++) { add(i, 4); }
    for (i = 11; i <= 17; i++) { add(i, 2); }
    for (i = 18; i <= 25; i++) { add(i, 1); }
    return cards;
  }

  function shuffle(from) {
    var shuffled = [];
    while (from.length > 0) {
      shuffled.push(from.splice(Math.random()*from.length,1));
    }
    return shuffled;
  }

  function take(count, from) {
    var taken = [];
    for (var i = 0; i < count; i++) {
      taken.push(from[i]);
    }
    return taken;
  }

  this.hand = take(6, shuffle(deck()));
  return this.hand;
};
  
var okToGuess = function(state) {
  return state.guesser === null; 
};

var checkSolution = function(state, user, expression) {
  // calculator funciton, given two numbers and an operator, it'll perform
  // the requested op. If the result is negative, or if either number is less
  // than 0, null is returned.

  var calc = function(a,op,b) {
    var result = -1;
    if (op === "+") {
      result = a + b;
    } else if (op === "-") {
      result = a - b;
    } else if (op === "/") {
      if (b === 0) 
        return null;
      var q = a / b;
      result = q;
      if (b * q != a)
        return null;
    } else if (op == "*") {
      result = a * b;
    } 
    if (result < 0 || a < 0 ||  b < 0)
      return null;
    return result;
  };
  var isDigit = function (digit) {
    return (!isNaN(parseInt(digit)));
  };
  var evalInfix = function(tokens) {
    // convert infix to postfix
    var ps = ["+","-","/","*"];
    var operators = {
      '+' : [0,"left"],
      '-' : [0,"left"],
      '*' : [1,"left"],
      '/' : [1,"left"],
    };
    var out = [];
    var stack = [];
    for (var index= 0; index< tokens.length; index++) {
      var token = tokens[index]; 
        if (isDigit(token)) {
          out.push(parseInt(token));
        }
        if (ps.indexOf(token) != -1) {
          while (stack.length != 0 && 
                 ps.indexOf(stack[stack.length - 1]) != -1  &&
                 (operators[token][1] == "left" &&
                  operators[token][0] <= operators[stack[stack.length - 1]][0] ||
                  operators[token][0] < operators[stack[stack.length - 1]][0])) {
             out.push(stack.pop());
           }
          stack.push(token);
        } else if (token == '(') {
          stack.push(token);
        } else if (token == ')') {
          while (stack.length > 0 &&  stack[stack.length - 1] != "(") {
              out.push(stack.pop());
          }
          if (stack.length === 0) {
            return null;//# Error mismatched.
          }
          if (stack[stack.length - 1] == "(") {
            stack.pop();
          }
        }
    }
    while (stack.length) {
      if ( ["(",")"].indexOf( stack[stack.length - 1]) != -1) {
        return null;
      }
      out.push(stack.pop());
    }
    return evalPostfix(out);
  };

  var evalPostfix = function(postfix){
  // eval postfix express, tokens is an array of tokens.
    var ps = ["+","-","/","*"];
    var stack = [];
    var out = postfix;
    for (var i = 0; i < out.length; i++ ) {
      var o = out[i]; 
      if (ps.indexOf(o) !== -1) {
        var b = parseInt(stack.pop());
        var a = parseInt(stack.pop());
        var c  = calc(a,o,b);
        if (c === null)
          return 0;
        stack.push(c);
      } else if (isDigit(o))  {
        stack.push(o);
      }
    }

    if (stack.length > 1)
      return null;
    return parseInt(stack.pop());

  };

  if (state.guesser != user) {
    return 0;
  }

  var tokens = util.regex_match(/([\(\)\+\-\*\/]|\d+)/g,expression);
  var numbers = util.regex_match(/(\d+)/g,expression);
  numbers = numbers.slice(0,5).sort();
  var cards = state.hand.slice(0,5).sort();
  var correct = (numbers.length == cards.length) && 
                numbers.every(function(element, index) {
    return element == cards[index]; 
  });

  if (!correct)
    return 0;
  var solution = evalInfix(tokens);
  correct = (correct && solution && state.hand[5][0] === solution) ;
  return correct;
};

Krypto.prototype.solve = function (cards, objective) {
  var solutions = [];

  function evaluate(nums, ops) {
    if (nums.length == 1) {
      return nums[0];
    } else {
      var op = toOp(ops[0]);
      var next = evaluate(nums.slice(1), ops.slice(1));
      if (next < 0 || next != Math.round(next)) {
        return -999;
      }
      return op(nums[0], next);
    }
  }

  function toOp(opc) {
    function add(a, b) { return a + b; };
    function sub(a, b) { return a - b; };
    function mul(a, b) { return a * b; };
    function div(a, b) { return a / b; };
         if (opc == '+') { return add; }
    else if (opc == '-') { return sub; }
    else if (opc == '*') { return mul; }
    else if (opc == '/') { return div; }
  }

  function permute(xs) {
    if (xs.length == 0) {
      return [[]];
    } else {
      var allPerms = [];
      for (var i = 0; i < xs.length; i++) {
        var subPerms = permute(xs.slice(0,i).concat(xs.slice(i+1,xs.length)));
        for (var j = 0; j < subPerms.length; j++) {
          allPerms.push([xs[i]].concat(subPerms[j]));
         }
      }
      return allPerms;
    }
  }

  function show(nums, ops) {
    if (nums.length == 2) {
      return nums[0] + ' ' + ops[0] + ' ' + nums[1];
    } else {
      return nums[0] + ' ' + ops[0] + ' (' + show(nums.slice(1), ops.slice(1)) + ')';
    }
  }

  var ops = ['+','-','*','/'];
  var opses = [];
  for (var i = 0; i < 4; i++) {
    for (var j = 0; j < 4; j++) {
      for (var k = 0; k < 4; k++) {
        for (var l = 0; l < 4; l++) {
          opses.push([ops[i], ops[j], ops[k], ops[l]]);
        }
      }
    }
  }
  var numses = permute(cards);
  for (var n in numses) {
    for (var o in opses) {
      if (objective == evaluate(numses[n], opses[o])) {
        solutions.push(show(numses[n], opses[o]));
      }
    }
  }

  return solutions;
};

exports.checkSolution = checkSolution;
exports.okToGuess = okToGuess;
exports.Krypto = Krypto;
