# Sectery [![Build Status](https://travis-ci.org/earldouglas/sectery.svg?branch=master)](https://travis-ci.org/earldouglas/sectery) [![Coverage Status](https://coveralls.io/repos/earldouglas/sectery/badge.png)](https://coveralls.io/r/earldouglas/sectery)

## Usage

Copy *config.json.example* to *config.json*, replacing the various *test* values with your own.

Install the dependencies:

```
npm install
```

Fire up Sectery:

```
node sectery
```

## Development

The tests utilize the included *config.json.example*, so there's no need to copy it to *config.json* for development.

### Run the tests

```
npm test
```

Observe that they all pass.

### Add a new test

For the feature you'd like to develop, write a new test for it in *test/sectery_test.js*:

```javascript
'emoji': function(test) {
  test.expect(2);
  client._message('testuser', '#test-channel', 'foo bar table flip baz');
  test.equal(client._lastSaid().to, '#test-channel');
  test.equal(client._lastSaid().message, '╯°□°）╯︵ ┻━┻');
  test.done();
},
```

### Run the tests again

```
npm test
```

Observe that your new test fails.

### Make your test pass

*lib/plugins/emoji.js:*

```javascript
'use strict';

function listener(client) {
  return function(from, to, message) {
    if (/table\s*flip/i.test(message)) {
      client.say(to, '╯°□°）╯︵ ┻━┻');
    }
  };
}

module.exports.event    = 'message';
module.exports.listener = listener;
```

### Add optional configuration

Configuration is loaded by plugin name from *config.json* (or *config.json.example* during testing), and passed in as the second argument to `listener()` in each plugin.

To add configuration to the above *emoji* plugin, add an argument for its configuration:

*lib/plugins/emoji.js:*

```
function listener(client, config) {
  console.log(config.foo.bar);
```

Then create some sample configuration:

*config.json.example:*

```javascript
{

  // ...

  "plugins": {
    // ...
    "emoji": {
      "config": {
        "foo": {
          "bar": 42
        }
      }
    }
  }
}
```
