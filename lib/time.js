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

function next_end_work_day(d) {
  var now = d;
  if (!d)
    now = new Date();
  var end = new Date(now);
  end.setHours(17);
  end.setMinutes(0);

  //after 5pm or it's saturday or sunday, find the next workday.
  if (now.getHours() >= 17 || [6,0].indexOf(now.getDay()) !== 1 ) {
    var daystillMonday = (8 - now.getDay()) % 7;
    end = new Date( end.getTime() + (daystillMonday * 24 * 60 * 60 * 1000)); 
  }
  return time_delta(now,end);
}

function pluralize(num, str) {
 if (num > 1) 
  return str + 's';
 return str;
}


module.exports.time_delta = time_delta;
module.exports.next_end_work_day = next_end_work_day;
