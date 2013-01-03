(function() {

function init() {
    var elements = {
        'system': document.getElementById('timestamper-systemTime'),
        'elapsed': document.getElementById('timestamper-elapsedTime'),
        'none': document.getElementById('timestamper-none')
    }

    getCookie(elements);

    for (var key in elements) {
        elements[key].addEventListener('click', function() {
            setCookie(elements);
        }, false);
    }
}

function setCookie(elements) {
    for (var key in elements) {
        if (elements[key].checked) {
            document.cookie = 'timestamper=' + key;
            document.location.reload();
            return;
        }
    }
}

function getCookie(elements) {
    var result = /(?:^|;\\s*)timestamper\s*=\s*([^;]+);/.exec(document.cookie);
    if (result === null) {
        elements['system'].checked = true;
        return;
    }
    var element = elements[result[1]];
    if (element) {
        element.checked = true;
    }
}

new Ajax.Updater(
    document.getElementById('navigation'),
    rootURL + "/extensionList/hudson.console.ConsoleAnnotatorFactory/hudson.plugins.timestamper.annotator.TimestampAnnotatorFactory/usersettings",
    { insertion: Insertion.After, onComplete: init }
);

}());