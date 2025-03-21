var _hh_jsEnv = {
    ios: false,
    android: true,
    ipad: false,
    hideAd: false,
    domChgCooldown: 1000,
    domChgMergeMax: 5,
    enableConsole: true,
};

function _hh_log(s) {
    if (window.webkit) { // iOS
        window.webkit.messageHandlers.ddLogger.postMessage(s);
    }
    if (window.androidCtx) { // Android
        window.androidCtx.wvJsLog(s);
    }

    if (_hh_jsEnv.enableConsole) {
        console.log(s);
    }
}

function _hh_domObserverSetup() {
    if (document._hh_observer != null) {
        return;
    }

    var sendDomUpdEventMsg = function (count) {
        const msg = (count + " changes, withheld: " + document._hh_obsCount);
        if (_hh_domObserverCallback) {
            _hh_domObserverCallback(msg);
        } else {
            _hh_log("_hh_domObserverCallback not found in JS context, using native bridging callback if found.");
            if (window.webkit.messageHandlers.hhDomObs) {
                window.webkit.messageHandlers.hhDomObs.postMessage(msg);
            }
        }
        document._hh_obsCount = 0;
        document._hh_obsTimestamp = new Date();
    }

    var observer = new MutationObserver(function (mutations) {
        var c = mutations.length;
        if (document._hh_obsCount + c >= _hh_jsEnv.domChgMergeMax) {
            sendDomUpdEventMsg(c);
            return;
        }

        var now = new Date();
        var timeDiff = now.getTime() - document._hh_obsTimestamp.getTime();
        if (timeDiff > _hh_jsEnv.domChgCooldown && document._hh_obsCount + c > 0) {
            sendDomUpdEventMsg(c);
            return;
        }

        document._hh_obsCount += c;
    });

    observer.observe(document, { childList: true, subtree: true });
    document._hh_observer = observer;
    document._hh_obsCount = 0;
    document._hh_obsTimestamp = new Date();
    _hh_log("dom observer setup done.");
}


// leaf node first, root node last.
function _hh_elemArrayToRoot(elem, outArray) {

    if (elem == null || elem.parentNode == null) {
        return outArray;
    }

    outArray.push(elem);
    _hh_elemArrayToRoot(elem.parentNode, outArray);
    return outArray;
}

// elemStackArr = leaf node first, root node last, for "A -> B -> C -> D -> E", would be ["E", "D", "C", "B", "A"]
// searchStopAtElemTagName = "C"
// keepLeafLevel = 1
// result = ["D", "C", "B", "A"], i.e, "A -> B -> C -> D"
function _hh_trimElemStack(elemStackArr, searchStopAtElemTagName, keepLeafLevel) {
    var stopAt = -1;

    for (var i = 0; i < elemStackArr.length; i++) {
        var node = elemStackArr[i];
        var tagName = node.tagName;
        if (tagName.toUpperCase() === searchStopAtElemTagName.toUpperCase()) {
            stopAt = i;
            break;
        }
    }

    if (stopAt < 0) {
        return null;
    }

    var subRangeStart = stopAt - keepLeafLevel;
    if (subRangeStart >= 0 && subRangeStart < elemStackArr.length) {
        return elemStackArr.slice(subRangeStart);
    } else {
        return null;
    }
}

const _hh_AdTaggingColorStyles = [
    "background:#99FFFF;opacity:0.3;",
    "background:#FFFF99;opacity:0.3;",
    "background:#FF99FF;opacity:0.3;",
    "background:#FF9999;opacity:0.3;",
    "background:#9999FF;opacity:0.3;",
    "background:#99FF99;opacity:0.3;",
];

const _hh_AdElementHiddenStyle = "display:none;";

function _hh_getAdStyle() {
    return _hh_jsEnv.hideAd ? _hh_AdElementHiddenStyle : _hh_AdTaggingColorStyles[0];
}

function _hh_simulateClick(skipButton) {
    if (!window.androidCtx) {
        return;
    }
    var rect = skipButton.getBoundingClientRect();
    var bounds = {
        "x": Math.round(rect.left),
        "y": Math.round(rect.top),
        "width": Math.round(rect.width),
        "height": Math.round(rect.height)
    };
    if (bounds.width > 0) {
        window.androidCtx.simulateClick(JSON.stringify(bounds));
    }
}

// 2022.8.22 by zhangjian
// detect ytb video play status for PIP
function _hh_videoStatusChanged(playing, width, height) {
    if (window.androidCtx) {
        window.androidCtx.videoStatusChanged(playing, width, height);
    }
}

function _hh_updAdCounter(flag = false) {
    _hh_log('update ad count for url: ' + window.location.href);

    if (window.androidCtx) { // Android
        androidCtx.counter(window.location.href, flag);
    }

    if (window.webkit) { // iOS
        //TODO
    }
}

_hh_domObserverSetup();

function _hh_openLink(event) {
    event.preventDefault();
    if (window.androidCtx) {
        window.androidCtx.wvJsOpenLink(this.href);
    }
}

links = document.querySelectorAll("a");
links.forEach(a => {
    a.addEventListener("click", _hh_openLink);
})
