# Sectery [![Build Status](https://travis-ci.org/earldouglas/sectery.svg?branch=master)](https://travis-ci.org/earldouglas/sectery) [![Coverage Status](https://coveralls.io/repos/earldouglas/sectery/badge.png)](https://coveralls.io/r/earldouglas/sectery)

## Usage

Install the dependencies:

```
npm install
```

Fire it up:

```
node sectery
```

## Development

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

*lib/listeners/message/emoji.js:*

```javascript
'use strict';

function messageListener(db, from, channel, message) {
  if (/table\s*flip/.test(message) || /flip\s*table/.test(message)) {
    return [ { to: channel, message: '╯°□°）╯︵ ┻━┻' } ];
  }
}

module.exports = messageListener;
```
