'use strict';

var fs = require('fs');
var nconf = require('nconf');
var savetime = 60000;  // 1 minute: 60 seconds * 1000 ms / 1 second

function loadPlugins(dir, configFile, k) {

  nconf.env().file(configFile);

  nconf.set(
    'plugins:weather:api:key',
    nconf.get('WU_API_KEY') || nconf.get('plugins:weather:api:key')
  );

  fs.readdirSync(__dirname + '/' + dir + '/').forEach(function(file) {
    var name = './' + dir + '/' + file;
    var plugin = require(name);
    var pluginName = file.replace(/\.js/, '');

    var pluginConfig = nconf.get('plugins:' + pluginName) ||
                       nconf.get('common') ||
                       {};
    pluginConfig.set = function(table, key, value) {
      nconf.set(pluginName + ':' + table + ':' + key, value);
      nconf.save(function (err) { if (err) { return; } });
    };
    pluginConfig.getNicknames = function(channel) {
      
      var nicks = nconf.get('nicks');
      var activeNicks = [];
      
      Object.keys(nicks[channel]).forEach(function(element) {
        if (nicks[channel][element]) {
          activeNicks.push(element);
        }
      });
      return activeNicks;
    };
    pluginConfig.clear = function(table, key) {
      nconf.reset(pluginName + ':' + table + ':' + key);
    };
    pluginConfig.getConfig = function (pluginName) {
      return nconf.get('plugins:' + pluginName) ||
                       nconf.get('common') ||
                       {};
    };
    k(pluginName, pluginConfig, plugin);
  });
}
function loadListeners(client, configFile) {

  var commands = { pm: {}, message: {}};

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

    var time = nconf.get('common:savetime') || {};
    if ( Date.now() - time > savetime) {
      nconf.save(function (err) { if (err) { return; } });
    }
  });

  loadPlugins('plugins', configFile, function (name, config, plugin) {
    client.addListener(plugin.event, plugin.listener(client, config));
  }); 

  
  client.addListener('message', function(from, to, message) {
    if (/^@help/.test(message)) {
      client.say(to, 'Available commands: @help, @scala, @tell, @date, @krypto, @note, @translate, @sms, @weather, @setup');
    }
  });
  client.addListener('join', function(channel, nick, message) {
    var names = nconf.get('nicks:' + channel);
    nconf.set('nicks:' + channel + ':' + nick, true);
  });
  client.addListener('part', function(channel, nick, reason, message) {
    var names = nconf.get('nicks:' + channel);
    nconf.set('nicks:' + channel + ':' + nick, false);
  });
  client.addListener('names', function(channel, names) {
    var activeNicks = {};
    Object.keys(names).forEach(function(element) {
      activeNicks[element] = true;
    });
    nconf.set('nicks:' + channel, activeNicks);
  });
}

module.exports = loadListeners;
