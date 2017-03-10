# Sectery [![Build Status](https://travis-ci.org/earldouglas/sectery.svg?branch=master)](https://travis-ci.org/earldouglas/sectery) [![Coverage Status](https://coveralls.io/repos/github/earldouglas/sectery/badge.svg?branch=master)](https://coveralls.io/github/earldouglas/sectery?branch=master)

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
npm install
npm test
```

Observe that they all pass.

### Add a new test

For the feature you'd like to develop, write a new test for it in
*test/sectery_test.js*:

```javascript
describe('message listeners', function () {

  // ...

  testIO('emoji', 'table flip', '╯°□°）╯︵ ┻━┻');

});
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
