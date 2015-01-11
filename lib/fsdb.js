'use strict';

var fs = require('fs');

function load(name) {
  var s = fs.readFileSync(name);
  return JSON.parse(s);
}

function save(name, data) {
  var json = JSON.stringify(data, null, 2);
  fs.writeFileSync(name, json);
}

function set(name, key, value) {
  var o = load(name);
  o[key] = value;
  save(name, o);
}

module.exports.load = load;
module.exports.save = save;
module.exports.set  = set;
