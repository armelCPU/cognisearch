var page = require('webpage').create(),
  system = require('system'),
  t, address;

if (system.args.length < 1) {
  console.log('Usage: phantom_script.js <some URL>');
  phantom.exit();
}

address = system.args[1];
page.open(address, function(status) {
  if (status !== 'success') {
    //console.log('FAIL to load the address' + address);
  } else {
    var doc = page.evaluate(function() {
        return document.getElementsByTagName("HTML")[0].innerHTML;
      });

    // console.log(doc);
  }
  phantom.exit();
});