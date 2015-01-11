'use strict';

var fs = require('fs');
var fsdb = require('./fsdb');

function removeListeners(client) {
  ['error', 'pm', 'message', 'join'].forEach(function (event) {
    client.listeners(event).forEach(function (listener) {
      client.removeListener(event, listener);
    });
  });
}

function addListeners(client, configFile) {

  var config = fsdb.load(configFile);

  function safeAddListener(event, listener) {
    client.addListener(event, function() {
      try {
        listener.apply(this, arguments);
      } catch (e) {
        console.error(e);
      }
    });
  }

  fs.readdirSync(__dirname + '/plugins/').forEach(function(file) {
    var name = './plugins/' + file;
    var resolved = require.resolve(name);
    delete require.cache[resolved];
    var plugin = require(name);
    var pluginConfig = {};
    Object.keys(config.common).forEach(function (key) {
      pluginConfig[key] = config.common[key];
    });
    var pluginName = file.replace(/\.js/, '');
    if (config.plugins[pluginName] && config.plugins[pluginName].config) {
      pluginConfig = config.plugins[pluginName].config;
    }
    pluginConfig.set = function(table, key, value) {
      config.plugins[pluginName].config[table][key] = value;
      fsdb.save(configFile, config);
    };
    pluginConfig.clear = function(table, key) {
      delete config.plugins[pluginName].config[table][key];
      fsdb.save(configFile, config);
    };
    safeAddListener(plugin.event, plugin.listener(client, pluginConfig));
  });

}

function reloadListeners(client, configFile) {

  var config = fsdb.load(configFile);

  removeListeners(client, configFile);
  addListeners(client, configFile);

  client.addListener('pm', function(from, message) {
    if (from === config.common.admin && message === '@reload') {
      reloadListeners(client, configFile);
      client.say(from, 'Reloaded.');
    }
  });

  client.addListener('message', function(from, to, message) {
    if (/^@help/.test(message)) {
      client.say(to, 'Available commands: @help, @scala, @tell, @date, @krypto, @note, @translate, @sms, @weather');
    }
  });

}

module.exports = reloadListeners;
