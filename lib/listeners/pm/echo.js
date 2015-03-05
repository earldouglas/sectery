'use strict';

function pmListener(db, from, message) {
  var match = /^@echo\s+(.+)$/.exec(message);
  if (match) {
    return [ { to: from, message: match[1] } ];
  }
}

module.exports = pmListener;
