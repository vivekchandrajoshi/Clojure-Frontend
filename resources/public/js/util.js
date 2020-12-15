// The function to handle actual exporting a svg node to string:
function getSVGString(svgNode) {
  svgNode.setAttribute('xlink', 'http://www.w3.org/1999/xlink');
  var cssStyleText = getCSSStyles(svgNode);
  appendCSS(cssStyleText, svgNode);
  var serializer = new XMLSerializer();
  var svgString = serializer.serializeToString(svgNode);
  svgString = svgString.replace(/(\w+)?:?xlink=/g, 'xmlns:xlink='); // Fix root xlink without namespace
  svgString = svgString.replace(/NS\d+:href/g, 'xlink:href'); // Safari NS namespace fix
  return svgString;
  function getCSSStyles(parentElement) {
    var selectorTextArr = [];
    // Add Parent element Id and Classes to the list
    selectorTextArr.push('#' + parentElement.id);
    for (var c = 0; c < parentElement.classList.length; c++)
      if (!contains('.' + parentElement.classList[c], selectorTextArr))
        selectorTextArr.push('.' + parentElement.classList[c]);
    // Add Children element Ids and Classes to the list
    var nodes = parentElement.getElementsByTagName("*");
    for (var i = 0; i < nodes.length; i++) {
      var id = nodes[i].id;
      if (!contains('#' + id, selectorTextArr))
        selectorTextArr.push('#' + id);
      var classes = nodes[i].classList;
      for (var c = 0; c < classes.length; c++)
        if (!contains('.' + classes[c], selectorTextArr))
          selectorTextArr.push('.' + classes[c]);
    }
    // Extract CSS Rules
    var extractedCSSText = "";
    for (var i = 0; i < document.styleSheets.length; i++) {
      var s = document.styleSheets[i];

      try {
        if (!s.cssRules) continue;
      } catch (e) {
        if (e.name !== 'SecurityError') throw e; // for Firefox
        continue;
      }
      var cssRules = s.cssRules;
      for (var r = 0; r < cssRules.length; r++) {
        if (contains(cssRules[r].selectorText, selectorTextArr))
          extractedCSSText += cssRules[r].cssText;
      }
    }

    return extractedCSSText;
    function contains(str, arr) {
      return arr.indexOf(str) === -1 ? false : true;
    }
  }
  function appendCSS(cssText, element) {
    var styleElement = document.createElement("style");
    styleElement.setAttribute("type", "text/css");
    styleElement.innerHTML = cssText;
    var refNode = element.hasChildNodes() ? element.childNodes[0] : null;
    element.insertBefore(styleElement, refNode);
  }
}


// The function to take an svg string and generate the image blob
// which is returned back though a callback
function svgString2Image(svgString, width, height, callback) {
  var canvas = document.createElement("canvas");
  canvas.width = width;
  canvas.height = height;
  canvg(canvas, svgString, {
    ignoreMouse: true,
    ignoreAnimation: true,
    ignoreDimensions: true
  });
  canvas.toBlob(function (blob) {
    if (callback) callback(blob);
  });
}

function _base64ToArrayBuffer(base64) {
    var binary_string =  window.atob(base64);
    var len = binary_string.length;
    var bytes = new Uint8Array( len );
    for (var i = 0; i < len; i++)        {
        bytes[i] = binary_string.charCodeAt(i);
    }
    return bytes.buffer;
}


function getBuffer(url) {
  var base64str = url.replace('data:image/png;base64,','')
  return _base64ToArrayBuffer(base64str);
}
