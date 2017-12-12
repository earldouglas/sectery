var curl = require('../../curl');
var moment = require('moment-timezone');
var time = require('../../time.js');

function messageListener(db, from, channel, message, reply) {

  var nextCommand = function() {
    var url = 'https://api.spacexdata.com/v2/launches/upcoming';
    curl(url, function (res) {
      //console.log('spacex response:', res);
      var nextFlight = res[0];
      var nextFlightTime = nextFlight.launch_date_utc;
      var tz = time.getTimezone(db,from);
      var launchTime = moment.utc(nextFlightTime).tz(tz).format("MM-DD-YYYY hh:mm:ss z");

      // Flight <date> scheduled for <date> 
      var msg = ['Flight', 
                 nextFlight.flight_number,
                 'scheduled for',
                 launchTime + '.'];
      console.log(msg.join(' '));
      reply({ to: channel, message: msg.join(' ')});

      var rocket = nextFlight.rocket;
      var payload = rocket.second_stage.payloads[0];
      var core = rocket.first_stage.cores[0];

      // [New|Previously flown] Falcon 9 carrying Dragon 1.1 to ISS launching from [long name]
      msg = [];
      if (core.reused) {
        msg.push('Previously flown');
      } else {
        msg.push('New');
      }
      msg = msg.concat([rocket.rocket_name,
                  'carrying',
                  payload.payload_type,
                  'to',
                  payload.orbit,
                  'launching from',
                  nextFlight.launch_site.site_name_long + '.']);

      console.log(msg.join(' '));
      reply({ to: channel, message: msg.join(' ')});

      // Attempting landing on LZ-1.
      msg = [];
      if (core.landing_type in {'ASDS':{},'RTLS':{}}) {
        msg.push('Attempting landing on');
        msg.push(core.landing_vehicle);
      } else {
        msg.push('Expendable');
      }

      console.log(msg.join(' '), core.landing_type);
      reply({ to: channel, message: msg.join(' ')});
    });
  };

  if (/^@spacex/.test(message)) {
    var commandMatch = /^@spacex\s+(.*)$/.exec(message);

    if (commandMatch) {

      var command = commandMatch[1];
      console.log('spacex command:', command);
      switch ( command ) {
        case 'next':
          nextCommand();
          break;
        default:
         reply({ to: channel, message: 'command "' + command + '" not recognized.' });
      }
    } else {
      reply({ to: channel, message: '@spacex <next>' });
    }
  } 

}

module.exports = messageListener;
module.exports.help = [{ cmd:'@spacex',
                         syntax: '@spacex <command>',
                         output: {success: ['next: next scheduled launch info.'],
                                  failure: []}
                       }];

