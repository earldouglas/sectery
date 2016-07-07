'use strict';

var fs = require('fs');
var teep = require('teep');

function loadListeners(eventName) {
  var listeners = [];
  fs.readdirSync(__dirname + '/listeners/' + eventName + '/').forEach(function(file) {
    if (/^[^\.].*.js$/.test(file)) {
      var path = './listeners/' + eventName + '/' + file;
      listeners.push(require(path));
    }
  });
  return listeners;
}

var dbFile = process.env.HOME + '/.sectery.json';

function getDb() {
  if (fs.existsSync(dbFile)) {
    return JSON.parse(fs.readFileSync(dbFile));
  } else {
    return {};
  }
}

function saveDb(db) {
  fs.writeFileSync(dbFile, JSON.stringify(db, null, 2));
}

function sectery(client) {

  var db = getDb();
  db.cron = [];
  db.poll = db.poll || [];
  db.poll['#sectery'] = {id:0, polls:[]};

  var sendReply = teep.fn.throttle(5, 10000, 250, function (reply) {
    if (reply && reply.callback) {
      reply.callback(client.say.bind(client));
    } else if (reply && reply.to && reply.message) {
      client.say(reply.to, reply.message);
    }
  });

  var ralph = setInterval(function () {
    var replies = [
      "If mommy's purse didn't belong in the microwave, why did it fit?",
      "I wet my arm pants.",
      "Grandma had hair like that when she went to sleep in her forever box!",
      "All my friends have birthdays this year!",
      "I'm bembarassed for you.",
      "My knob tastes funny.",
      "Hi, Principal Skinner! Hi, Super Nintendo Chalmers.",
      "And I want a bike and a monkey and a friend for the monkey.",
      "Eww, Daddy, this tastes like Gramma!",
      "I bent my wookie.",
      "Lisa's bad dancing makes my feet sad.",
      "That's where I saw the Leprechaun. He tells me to burn things.",
      "Look Big Daddy, it's Regular Daddy.",
      "Look, Daddy, a whale egg!",
      "Daddy, I'm scared. Too scared to wet my pants.",
      "My cat's name is Mittens.",
      "This snowflake tastes like fish sticks.",
      "My parents won't let me use scissors.",
      "Slow down, Bart! My legs don't know how to be as long as yours.",
      "When I grow up I wanna be a Principal or a Caterpillar.",
      "Principal Skinner, I got carsick in your office.",
      "Dear Miss Hoover, you have Lyme disease. We miss you. Kevin is biting me. Come back soon. Here's a drawing of a spirokeet. Love Ralph",
      "Bushes are nice 'cause they don't have prickers. Unless they do. This one did. Ouch!",
      "I dress myself.",
      "This is my sandbox, I'm not allowed to go in the deep end.",
      "The doctor told me that BOTH my eyes were lazy! And that's why it was the best summer ever.",
      "I kissed a light socket once and I woke up in a helicopter!",
      "My cat's breath smells like cat food.",
      "He's gonna smell like hot dogs.",
      "Miss Hoover, I glued my head to my shoulders.",
      "When I grow up I'm going to Bovine University.",
      "I ate too much plastic candy.",
      "I ate all my caps...ow!",
      "I found a moon rock in my nose!",
      "I'm wearing a bathrobe, and I'm not even sick.",
      "Will you cook my dinner for me? My parents aren't around and I'm not allowed to turn on the stove.",
      "You have the bestest Dad. He read me a story about Chinese food.",
      "Miss Hoover, there's a dog in the vent.",
      "Me fail English? That's unpossible.",
      "I'm a furniture!",
      "My face is on fire.",
      "The doctor said I wouldn't have so many nosebleeds if I kept my finger out of there.",
      "Your hair is tall...and pretty!",
      "Wheeee... ow I bit my tongue.",
      "It tastes like ... burning.",
      "Oh boy, sleep! That's where I'm a Viking!",
      "Was President Lincoln okay?",
      "I'm Idaho!",
      "And when the doctor said I didn't have worms any more, that was the happiest day of my life.",
    ];
    var index = Math.floor(Math.random() * replies.length);
    sendReply({ to: '##SuperBestFriendsHappyFunClub', message: replies[index] });
  }, 98 * 60 * 1000);

  function sendReplies(replies) {
    if (replies) {
      replies.forEach(function (reply) {
        sendReply(reply);
      });
    }
    saveDb(db);
  }

  client.addListener('join', function (channel, nick, message) {
    function run(joinListener) {
      sendReplies(joinListener(db, channel, nick, message));
    }
    loadListeners('join').forEach(run);
  });

  client.addListener('part', function(channel, nick, reason, message) {
    function run(partListener) {
      sendReplies(partListener(db, channel, nick, reason, message));
    }
    loadListeners('part').forEach(run);
  });

  client.addListener('names', function(channel, names) {
    function run(namesListener) {
      sendReplies(namesListener(db, channel, names));
    }
    loadListeners('names').forEach(run);
  });

  var messageCallback = function(from, to, message) {
    function run(messageListener) {
      try {
        sendReplies(messageListener(db, from, to, message,messageCallback));
      } catch (e) {
        console.log('running message listener', e);
      }
    }
    loadListeners('message').forEach(run);
  };

  client.addListener('message', messageCallback); 

  client.addListener('pm', function (from, message) {
    function run(pmListener) {
      sendReplies(pmListener(db, from, message));
    }
    loadListeners('pm').forEach(run);
  });

}

module.exports = sectery;
