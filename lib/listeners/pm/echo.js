'use strict';

module.exports = 
  function (db, from, message, reply) {
    var match = /^@echo\s+(.+)$/.exec(message);
    if (match) {
      reply({ to: from, message: match[1]});
    }
  };
