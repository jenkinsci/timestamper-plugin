(function() {

// Cookie is renewed each time the page is opened and expires after 2 years
// http://googleblog.blogspot.com.au/2007/07/cookies-expiring-sooner-to-improve.html

var cookieName = 'jenkins-timestamper';

function init() {
    var elements = {
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

    for (var key in elements) {
        elements[key].addEventListener('click', function() {
            onClick(elements);
        }, false);
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

function setCookie(cookie) {
    var d = new Date();
    d.setTime(d.getTime() + 1000 * 60 * 60 * 24 * 365 * 2); // 2 years
    document.cookie = cookieName + '=' + cookie + ";expires=" + d.toGMTString();
}

function getCookie() {
    var re = RegExp('(?:^|;\\s*)' + cookieName + '\s*=\s*([^;]+);');
    var match = re.exec(document.cookie);
    if (match) {
        return match[1];
    }
    return null;
}

new Ajax.Updater(
    document.getElementById('navigation'),
    rootURL + "/extensionList/hudson.console.ConsoleAnnotatorFactory/hudson.plugins.timestamper.annotator.TimestampAnnotatorFactory/usersettings",
    { insertion: Insertion.After, onComplete: init }
);

}());