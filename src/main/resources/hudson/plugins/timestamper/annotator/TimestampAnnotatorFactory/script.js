/*
 * The MIT License
 * 
 * Copyright (c) 2013 Steven G. Brown
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

(function() {

// Cookie is renewed each time the page is opened and expires after 2 years
// http://googleblog.blogspot.com.au/2007/07/cookies-expiring-sooner-to-improve.html

var cookieName = 'jenkins-timestamper';

function init() {
    var elements = {
        'local': document.getElementById('timestamper-localTime'),
        'system': document.getElementById('timestamper-systemTime'),
        'elapsed': document.getElementById('timestamper-elapsedTime'),
        'none': document.getElementById('timestamper-none')
    }

    elements['system'].checked = true;
    var cookie = getCookie();
    var element = elements[cookie];
    if (element) {
        element.checked = true;
        // renew cookie
        setCookie(cookie);
    }

    setOffsetCookie();

    for (var key in elements) {
        elements[key].observe('click', function() {
            onClick(elements);
        });
    }
}

function onClick(elements) {
    for (var key in elements) {
        if (elements[key].checked) {
            setCookie(key);
            document.location.reload();
            return;
        }
    }
}

function setOffsetCookie()
{
    var currentDate = new Date();
    var offset = currentDate.getTimezoneOffset();
    var offsetMS = offset * 60 * 1000;
    
    currentDate.setTime(currentDate.getTime() + 1000 * 60 * 60 * 24 * 365 * 2); // 2 years
    var attributes = "; path=/; expires=" + currentDate.toGMTString();
    document.cookie = cookieName + '-offset=' + offsetMS.toString() + attributes;
    
}

function setCookie(cookie) {
    var d = new Date();
    d.setTime(d.getTime() + 1000 * 60 * 60 * 24 * 365 * 2); // 2 years
    var attributes = "; path=/; expires=" + d.toGMTString();
    document.cookie = cookieName + '=' + cookie + attributes;
}

function getCookie() {
    var re = RegExp('(?:^|;\\s*)' + cookieName + '\s*=\s*([^;]+)');
    var match = re.exec(document.cookie);
    if (match) {
        return match[1];
    }
    return null;
}

var settingsInserted = false;

function timestampFound() {
    if (settingsInserted) {
        return;
    }
    settingsInserted = true;

    // for div layout in >= 1.572 we need to use 'side-panel-content'
    var element = document.getElementById('side-panel-content');
    if (null == element) {
        // for < 1.572 we need to use 'navigation'
        element = document.getElementById('navigation');
        if (null == element) {
            // element not found, so return to avoid an error (JENKINS-23867)
            return;
        }
    }

    new Ajax.Updater(
        element,
        rootURL + "/extensionList/hudson.console.ConsoleAnnotatorFactory/hudson.plugins.timestamper.annotator.TimestampAnnotatorFactory/usersettings",
        { insertion: Insertion.After, onComplete: init }
    );
}

Behaviour.register({
    "span.timestamp" : function(e) {
        timestampFound(e);
    }
});

}());
