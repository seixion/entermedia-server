// Generated by CoffeeScript 1.4.0
var em, fs, http, io, path, requestHandler;

em = {
  unit: {}
};

http = require('http');

fs = require('fs');

path = require('path');

requestHandler = function(request, response) {
  var content, fileName, localFolder, regex;
  fileName = path.normalize(request.url);
  regex = /(\.css)|(\.js)|(\.tpl\.html)|(\.jpg)|(\.png)/;
  fileName = regex.test(fileName) ? fileName : '/index.html';
  localFolder = __dirname;
  content = localFolder + fileName;
  fs.readFile(content, function(err, contents) {
    if (!err) {
      response.end(contents);
    } else {
      console.dir(err);
    }
    return em.unit;
  });
  return em.unit;
};

http = http.createServer(requestHandler);

io = require('socket.io')(http);

io.on('connection', function(socket) {
  socket.on('updateAnnotation', function(data) {
    console.log('updateAnnotation');
    socket.broadcast.emit('updateAnnotationResponse', data);
    return em.unit;
  });
  socket.on('removeAnnotation', function(data) {
    console.log('removeAnnotation');
    socket.broadcast.emit('removeAnnotationResponse', data);
    return em.unit;
  });
  return em.unit;
});

http.listen(3000, function() {
  console.log('listening on *:3000');
  return em.unit;
});
