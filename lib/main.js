'use strict';

var fs = require('fs');
var fsdb = require('./fsdb');

function loadPlugins(dir, configFile, k) {
  var config = fsdb.load(configFile);
  fs.readdirSync(__dirname + '/' + dir + '/').forEach(function(file) {
    var name = './' + dir + '/' + file;
    var resolved = require.resolve(name);
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
    k(pluginName, pluginConfig, plugin);
  });
}

function loadListeners(client, configFile) {

  var commands = { pm: {}, message: {}, };

  loadPlugins('commands', configFile, function (name, config, plugin) {
    commands[plugin.event][name] = plugin.listener(client, config);
  }); 

  client.addListener('pm', function (from, message) {
    var match = /^@(\w+)\s*(.*)$/.exec(message);
    if (match && commands.pm[match[1]]) {
      commands.pm[match[1]](from, match[2]);
    }
  });

  client.addListener('message', function (from, to, message) {
    var match = /^@(\w+)\s*(.*)$/.exec(message);
    if (match && commands.message[match[1]]) {
      commands.message[match[1]](from, to, match[2]);
    }
  });

  loadPlugins('plugins', configFile, function (name, config, plugin) {
    client.addListener(plugin.event, plugin.listener(client, config));
  }); 

  client.addListener('message', function(from, to, message) {
    if (/^@help/.test(message)) {
      client.say(to, 'Available commands: @help, @scala, @tell, @date, @krypto, @note, @translate, @sms, @weather');
    }
  });

}

module.exports = loadListeners;
