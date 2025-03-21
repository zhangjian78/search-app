_hh_jsEnv.domChgCooldown = 100;
_hh_jsEnv.domChgMergeMax = 2;

function _hh_domObserverCallback(msg) {
    // _hh_log("facebook DOM change got: " + msg);

    if (_hh_jsEnv.ipad) {
        _fb_cleanAdForPad();
    } else {
        // _fb_cleanAdWithAndroidUA();
        _fb_videoLoadStart();

        if (document.querySelector("article")) {
            [].slice.call(document.getElementsByTagName("div")).forEach(element => _fb_hideAdEle(element));
            [].slice.call(document.getElementsByTagName("header")).forEach(element => _fb_hideAdEle(element));
        }
    }
}

function _fb_isAdText(text) {
    if (!text) {
        return false;
    }

    if (text.startsWith("People You May Know")
        || text.startsWith("Suggested for you")
        || text.startsWith("Suggested Groups")
        || text.startsWith("Sponsored")) {
        return true;
    }

    return false;
}

var _hh_lastViewportHeight = -1;
var _hh_yOffset = -1;
var _hh_addStoryRemoved = false;
var _hh_foundVideo = false;
function _fb_cleanAdWithAndroidUA() {
    // _hh_log("_fb_cleanAdWithAndroidUA: ");
    var startTime = new Date();
    var login_top_banner = document.getElementById("login_top_banner")
    if (login_top_banner) { // 移除登录窗口提示
        login_top_banner.style = _hh_getAdStyle();
        return;
    }

    if (!_hh_addStoryRemoved) {
        _hh_addStoryRemoved = _fb_removeAddStory(); // Add to story
    }
    spans = document.querySelectorAll("span[class='f5']");
    // textarea = document.querySelector("textarea");
    /*if ((spans && spans.length < 20) || document.body.scrollHeight == _hh_lastViewportHeight) {
        return;
    }*/
    var screenRoot = document.querySelector("div[id=screen-root]");
    if (screenRoot) {
        if (Math.round(screenRoot.getBoundingClientRect().y) == _hh_yOffset) { //  && Math.round(screenRoot.getBoundingClientRect().height) == _hh_lastViewportHeight
            return;
        }
        _hh_yOffset = Math.round(screenRoot.getBoundingClientRect().y);
        _hh_lastViewportHeight = Math.round(screenRoot.getBoundingClientRect().height);
    }
    // _hh_lastViewportHeight = document.body.scrollHeight;

    if (!_hh_foundVideo) {
        _fb_videoLoadStart();
//        _hh_log("video: " + (null == video));
//        if (video) {
//            video.addEventListener('loadstart', _fb_videoLoadStart);
//            video.addEventListener('canplay', _fb_videoLoadStart);
//            video.addEventListener('pause', _fb_videoLoadStart);
//            video.addEventListener('progress', _fb_videoLoadStart);
//            // _hh_videoEvent = true;
//        }
    }

    _fb_removeDiscoverMore(); // Discover more groups
    _fb_removeSuggestedXXX(); // Suggested Videos/Groups/People You May Know
    var index = 0;
    spans.forEach(span => {
        text = span.innerText;
        // console.log(text);
        if (text.indexOf("People You May Know") > -1
            || text.indexOf("Suggested for you") > -1
            || text.indexOf("Suggested Groups") > -1
            || text.indexOf("Sponsored") > -1) {
            span.innerText = `${text.split("-")[0].trim()} - _hh_idx:${index}`;
            // console.log(index);
            // console.log(span);
            _fb_hideAdElement(span);
        }
        index++;
    });
    var log = `clean facebook ads, ${spans.length} elements used: ${((new Date() - startTime)/1000).toFixed(1)}s scrollHeight=${document.body.scrollHeight}/${_hh_lastViewportHeight}`;
    if (window._hh_log) {
        _hh_log(log);
    } else {
        console.log(log);
    }
}

function _fb_removeAddStory() {
    divs = document.querySelectorAll("div[class='fl ac']");
    for (i = 0; i < divs.length; i++) {
        text = divs[i].innerText;
        if (text.indexOf("Add to") > -1) {
            var element = _fb_findParent(divs[i]);
            if (!element) {
                return true;
            }
            element = _fb_findContainer(element);
            element.style = _hh_getAdStyle();
            return true;
        }
    }
    return false;
}

function _fb_removeDiscoverMore() {
    divs = document.querySelectorAll("div[class='fl ac']");
    index = 0;
    divs.forEach(div => {
        text = div.innerText;
        // console.log(text);
        if (text.indexOf("Discover more groups") > -1) {
            // div.innerText = `${text.split("-")[0].trim()} - _hh_idx:${index}`;

            element = _fb_findParent(div);
            while (element) {
                parent = element.parentNode;
                if (parent && parent.getBoundingClientRect().height < 600) {
                    // console.log(parent);
                    element = parent;
                    parent = element.parentNode;
                } else {
                    element.style = _hh_getAdStyle();
                    break;
                }
            }

        }
        index++;
    });
}

function _fb_removeSuggestedXXX() {
    divs = document.querySelectorAll("div[class='fl am']");
    index = 0;
    divs.forEach(div => {
        text = div.innerText;
        // console.log(text);
        if (text.indexOf("Suggested Videos") > -1
            || text.indexOf("People You May Know") > -1
            || text.indexOf("Suggested Groups") > -1) {
            // div.innerText = `${text.split("-")[0].trim()} - _hh_idx:${index}`;

            element = _fb_findParent(div);
            if (element) {
                element.style = _hh_getAdStyle();
            }
        }
        index++;
    });
}

function _fb_findParent(element) {
    var screenWidth = window.screen.width;

    // step 1 向上查找父亲容器
    var rect = element.getBoundingClientRect();

    if (rect.width == 0) { //宽度为0的时候，说明还未渲染
        return null;
    }

    while (rect.width < screenWidth) { //宽度不对，继续查找
        element = element.parentNode;
        rect = element.getBoundingClientRect();
    }
    return element;
}

function _fb_findNextSibling(element) {
    if (!element) {
        return null;
    }

    var right = element.nextSibling;
    while(!right) {
        element = element.parentNode;
        right = element.nextSibling;
    }
    return element;
}

function _fb_findNearBy(element) {
    elements = Array();
    if (!element) {
        console.log("element is null!!!");
        return elements;
    }
    if (element.previousSibling) {
        elements.push(element.previousSibling);
    }
    elements.push(element);
    var index = 0;
    var stop = false;
    while (index++ < 10 && !stop) {
        element = element.nextSibling;
        if (element) {
            var buttons = element.getElementsByTagName("button");
            // console.log(element);
            if (buttons.length > 2) {
                elements.push(element);
                stop = true;
                break;
            } else {
                elements.push(element);
            }
        } else {
            stop = true;
            break;
        }
    }
    return elements;
}

function _fb_findContainer(element) {
    var parent = element.parentNode;
    if (!parent) {
        return element;
    }

    while (true) {
        var h1 = element.getBoundingClientRect().height;
        var h2 = parent.getBoundingClientRect().height;
        if (h1 / h2 > .9) {
            element = parent;
            parent = element.parentNode;
            if (!parent) {
                return element;
            }
        } else {
            return element;
        }
    }
}

function _fb_hideAdElement(element) {
    // step 1 向上查找父亲容器
    element = _fb_findParent(element);
    // console.log(element);
    if (!element) {
        return;
    }

    // step 2 判断是否有右节点，没有则继续向上查找
    element = _fb_findNextSibling(element);
    // console.log(element);

    // step 3 将相邻节点全部隐藏
    var elements = _fb_findNearBy(element);
    // console.log(elements);

    elements.forEach(element => {
        //element.style = "background:#FFFF99;opacity:0.3;";
        element.style = _hh_getAdStyle();
        //element.remove();
    });
    return elements;
}

function _fb_cleanAdForPhone() {
    // [].slice.call(document.getElementsByTagName("div")).forEach(element => _fb_hideAdEle(element));
    // [].slice.call(document.getElementsByTagName("header")).forEach(element => _fb_hideAdEle(element));
    story = document.querySelector('#MStoriesTray')
    if (story) {
        story.style = _hh_getAdStyle();
        // _hh_updAdCounter(false);
    }

    header = document.getElementById("header-notices")
    if (header) {
        // header.style.display = "none";
    }

    articles = document.querySelectorAll('article');
    if (articles) {
        articles.forEach(article => {
            ad = false;
            headers = article.querySelectorAll('header')
            if (header) {
                headers.forEach(header => {
                    text = header.innerText.trim();
                    if (text.indexOf("People You May Know") > -1
                        || text.indexOf("Suggested for you") > -1
                        || text.indexOf("Suggested Groups") > -1
                        || text.indexOf("Sponsored") > -1) {
                        ad = true;
                        return
                    }
                })
            }

            if (ad && article.cssText != _hh_getAdStyle()) {
                article.style = _hh_getAdStyle();
                // _hh_updAdCounter(false);
            }
        });
    }
}

function _fb_videoLoadStart(event) {
    // console.log(`${event.type}\n`);
    video = document.querySelector("video");
    if (video) {
        video.style.background = "#000000";
        video.poster = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACQAAAAYCAIAAAAd2sgZAAAABGdBTUEAAK/INwWK6QAAABl0RVh0U29mdHdhcmUAQWRvYmUgSW1hZ2VSZWFkeXHJZTwAAAB2SURBVHjaYmCgIwAIILoigACiKwIIILoigACiKwIIILoigACiKwIIILoigACiKwIIILoigACiKwIIILoigACiKwIIILoigACiKwIIILoigACiKwIIILoigACiKwIIILoigACiKwIIILoigACiKwIIILoigAADAAo4AAHJNqTcAAAAAElFTkSuQmCC";
    }
}

function _fb_hideAdEle(node) {
    var text = node.innerText.trim();
    if (_fb_isAdText(text)) {
        _hh_log("remove elememt with text: " + text.substring(0, 20));
        var s = _hh_elemArrayToRoot(node, []);
        s = _hh_trimElemStack(s, "ARTICLE", 1);
        if (s != null && s.length > 0) {
            var a = s[0];
            a.style = _hh_getAdStyle();

            var r = a.nextSibling;
            while (r != null) {
                r.style = _hh_getAdStyle();
                r = r.nextSibling;
            }

            var p = a.previousSibling;
            while (p != null) {
                p.style = _hh_getAdStyle();
                p = p.previousSibling;
            }

        } else {
            _hh_log("FB AD removing failed, given rule not working.");
        }
    }
}

function _fb_cleanAdForPad() {
    var all_div = document.querySelectorAll("div[role=feed] > div");
    if (!all_div) {
        return;
    }

    all_div.forEach(div => {
        var span = div.querySelector("span[id]");
        if (!span) {
            return;
        }

        var text = span.innerText.trim();

        if (_fb_isAdText(text)) {
            div.style = _hh_getAdStyle();
            _hh_log("hiding AD with title: " + text);
        }
    });

    _fb_cleanAdForPhone();
    // [].slice.call(document.getElementsByTagName("div")).forEach(element => _fb_hideAdEle(element));
    // [].slice.call(document.getElementsByTagName("header")).forEach(element => _fb_hideAdEle(element));
}

if (!document.querySelector("article")) {
    _hh_jsEnv.domChgCooldown = 100;
    _hh_jsEnv.domChgMergeMax = 2;
    setInterval(_fb_cleanAdWithAndroidUA, 350);
} else {
    _hh_jsEnv.domChgCooldown = 800;
    _hh_jsEnv.domChgMergeMax = 5;
}

var orgOpen = window.open;
window.open = function (...args) {
    console.log(args[0]);
    if (window.androidCtx) { // Android
        window.androidCtx.wvJsOpenNewTab(args[0]);
    }
}