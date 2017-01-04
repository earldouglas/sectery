'use strict';

var request = require('sync-request');

var createList = function(name) {
  var postUrl = 'https://earldouglas.com/projects/check/api/lists';
  var body = request('POST', postUrl).getBody();
  var id = JSON.parse(body).id;

  var patchUrl = [ 'https://earldouglas.com/projects/check/api/lists/'
                 , id
                 , '?name='
                 , name
                 ].join('');
  request('PATCH', patchUrl);

  return id;
};

var addItem = function(id, text) {
  var url = [ 'https://earldouglas.com/projects/check/api/lists/'
            , id
            , '?text='
            , text
            ].join('');
  request('POST', url);
};

var fetchItems = function(id) {
  var url = [ 'https://earldouglas.com/projects/check/api/lists/'
            , id
            ].join('');
  var body = request('GET', url).getBody().toString();
  var items = JSON.parse(body).items;
  return items;
};

function messageListener(db, from, channel, message) {

  var messages = [];

  db.check = db.check || {};

  var show = /@check\s+show\s+([0-9A-Za-z-]+)\s*$/.exec(message);
  if (show) {
    var showName = show[1];
    if (db.check[showName]) {
      var showId = db.check[showName];
      var items = fetchItems(showId);
      for (var i = 0; i < items.length; i++) {
        messages.push({ to: channel, message: items[i].text });
      }
    } else {
      messages.push({ to: channel, message: 'No items.' });
    }
  }

  var add = /@check\s+add\s+([0-9A-Za-z-]+)\s+(.*)$/.exec(message);
  if (add) {
    var addName = add[1];
    var addId = db.check[addName] || createList(addName);
    db.check[addName] = addId;
    var text = add[2];
    addItem(addId, text);
    messages.push({ to: channel, message: 'Added.' });
  }

  return messages;
}

module.exports = messageListener;
module.exports.help = [ { cmd:'@check add'
                        , syntax: '@check add <list> <item>'
                        , output: { success: []
                                  , failure: []
                                  }
                        }
                      , { cmd:'@check show'
                        , syntax: '@check show <list>'
                        , output: { success: []
                                  , failure: []
                                  }
                        }
                      ];