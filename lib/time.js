'use strict';

function time_delta(date1, date2) {
  var units = [
    ['year',     12 * 4 * 7 * 24 * 60 * 60 * 1000],
    ['month',     4 * 7 * 24 * 60 * 60 * 1000],
    ['week',      7  * 24 * 60 * 60 * 1000],
    ['day',      24 * 60 * 60 * 1000],
    ['hour',     60 * 60 * 1000],
    ['minute',   60 * 1000],
    ['second', 1000]
      ];
  var diff = Math.abs(date1 - date2);
  var result = [];
  units.some(function(unit) {
    var msInTime = unit[0];
    var time = unit[1];

    var value = Math.floor(diff / unit[1]);
    diff -= value * unit[1];
    if (value > 0) 
      result.push(value, [pluralize(value,unit[0])]);
    if (diff <= 0 ) {
      return true;
    }
  }
  );
  return result.join(' ');
}

function pluralize(num, str) {
 if (num > 1) 
  return str + 's';
 return str;
}

module.exports.time_delta = time_delta;
