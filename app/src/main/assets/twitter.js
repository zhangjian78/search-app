
_hh_jsEnv.domChgCooldown = 200;
_hh_jsEnv.domChgMergeMax = 1;

function _hh_domObserverCallback(msg) {
    _hh_log("twitter DOM change got: " + msg);

    if (_hh_jsEnv.ipad) {
        _tw_cleanAdForPad();
    } else {
        _tw_cleanAdForPhone();
    }
}

function _tw_cleanAdForPhone() {
    // 暂时屏蔽，未发现作用域 zhj 7.24
    //    document.querySelectorAll('div[class="css-1dbjc4n r-1adg3ll r-1ny4l3l"]').forEach(div => {
    //        var remove = true;
    //        if (div.firstElementChild) {
    //            if (div.firstElementChild.getAttribute('class') == 'css-1dbjc4n') {
    //                remove = false;
    //            }
    //        }
    //        if (remove && div.style.cssText != _hh_getAdStyle()) {
    //            div.style = _hh_getAdStyle();
    //            _hh_updAdCounter(false);
    //        }
    //    });

    // 部分Promoted，并非全部，有可能是视频播放后的追踪ID
    document.querySelectorAll('div[data-testid="placementTracking"]').forEach(div => {
        var remove = true;
        if (div.firstElementChild) {
            if (div.firstElementChild.getAttribute('data-testid') == 'videoPlayer') {
                remove = false;
            }
        }

        if (remove && null != div.style && div.style.cssText != _hh_getAdStyle()) {
            div.style = _hh_getAdStyle();
            _hh_updAdCounter(false);
        }
    });

    _hh_log("remove twitter AD done(phone)!");
}

function _tw_cleanAdForPad() {
    document.querySelectorAll('article').forEach(article => {
        article.querySelectorAll('div[class="css-1dbjc4n r-1awozwy r-18u37iz r-1s2bzr4"]').forEach(div => {
            if (div.innerText.trim() == 'Promoted') {
                if (div.style.cssText != _hh_getAdStyle()) {
                    article.style = _hh_getAdStyle();
                    _hh_updAdCounter(false);
                }
            }
        });
    });

    _hh_log("remove twitter AD done(pad)!");
}
