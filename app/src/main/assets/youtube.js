_hh_jsEnv.domChgCooldown = 1000;
_hh_jsEnv.domChgMergeMax = 5;
_hh_jsEnv.lastDuration = 0;

function _hh_domObserverCallback(msg) {
    _hh_log("youtube DOM change got: " + msg);

    if (_hh_jsEnv.ios) {
        _ytb_cleanAdForIOS();
    }

    if (_hh_jsEnv.android) {
        _ytb_cleanAdForAndroid();
    }
}

function _ytb_iosInterceptAdVideo() {
    const _black_mp4 = 'data:video/mp4;base64,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';
    var eles = document.getElementsByTagName("video");
    var v = null;
    var h5MediaEleTest = false;
    if (eles == null || eles.length == 0) {
        _hh_log("no video element found.");
    } else if (eles.length == 1) {
        v = eles[0];
        h5MediaEleTest = v instanceof HTMLMediaElement;
    } else {
        _hh_log("more than one video element found, using the first one.");
        v = eles[0];
        h5MediaEleTest = v instanceof HTMLMediaElement;
    }

    _hh_log("found video player object? " + v + ", h5 test result? " + h5MediaEleTest);

    if (h5MediaEleTest == false) {
        return;
    }

    if (v.h5MediaEleTest != null) {
        return;
    }

    v.h5MediaEleTest = h5MediaEleTest;

    function handleEvent(event) {
        let eventName = event.type;
        _hh_log("handleEvent: " + eventName);
        let v = event.target;
        _hh_log('got player event: ' + eventName + ", duration: " + v.duration);
        let showLog = false;
        if (v.duration && v.duration > 0) {
            if (_hh_jsEnv.lastDuration > 0 && Math.abs(_hh_jsEnv.lastDuration - v.duration) / _hh_jsEnv.lastDuration > 0.1) { // 误差很小
                showLog = true;
            }
            _hh_jsEnv.lastDuration = v.duration;
        }
        checkMediaSrc(v, showLog);
        detectAndInterrupteAd(v);
    }

    function detectAndInterrupteAd(video) {

        if (video.src == null) {
            _hh_log('video has no src set, no checking now.');
            return;
        }

        let videoSrc = video.src;
        if (videoSrc.startsWith('data:video/mp4;base64,')) {
            _hh_log('dataObjURI detected, no more magic.');
            return;
        }

        if (video.duration < 20) {
            if (window.androidCtx) { // Android
                window.androidCtx.wvJsToast(videoSrc, '' + Math.round(video.duration));
            }

            _hh_log('AD detected, do magic here!');
            video.pause();
            video.src = _black_mp4;
            _hh_log('using dataObjURI, length: ' + video.src.length);
            video.play();
        }

//        if (videoSrc.includes('pltype=adhost')) {//content|contentugc adhost
//            _hh_log('AD detected, do magic here!');
//            video.pause();
//            video.src = _black_mp4;
//            _hh_log('using dataObjURI, length: ' + video.src.length);
//            video.play();
//        } else {
//            _hh_log('no AD detected, no magic here!');
//        }
    }

    function checkMediaSrc(video, b) {
        if (!b) {
            _hh_log('<===============video=================');
            _hh_log('src=' + video.src);
            _hh_log('srcObject=' + video.srcObject);
            _hh_log('readyState=' + video.readyState);
            _hh_log('buffered=' + video.buffered);
            _hh_log('networkState=' + video.networkState);
            _hh_log('currentSrc=' + video.currentSrc);
            _hh_log('duration=' + video.duration);
            _hh_log('ended=' + video.ended);
            _hh_log('error=' + video.error);
            _hh_log('videoTracks.length=' + video.videoTracks.length);
            _hh_log('audioTracks.length=' + video.audioTracks.length);
            _hh_log('textTracks.length=' + video.textTracks.length);
            _hh_log('===============video=================>');
        }

        if (window.androidCtx) { // Android
           window.androidCtx.wvJsVideoSrc(video.src, video.currentSrc, '' + video.duration);
        }
    }

    v.addEventListener('loadeddata', handleEvent);
    v.addEventListener('loadedmetadata', handleEvent);
    v.addEventListener('loadstart', handleEvent);
    v.addEventListener('progress', handleEvent);
    v.addEventListener('canplay', handleEvent);
    v.addEventListener('canplaythrough', handleEvent);
    v.addEventListener('playing', handleEvent);
    v.addEventListener('play', handleEvent);
    v.addEventListener('ended', handleEvent);
    v.addEventListener('canplay', handleEvent);
    v.addEventListener('durationchange', handleEvent);
    v.addEventListener('error', handleEvent);
    v.addEventListener('suspend', handleEvent);
    v.addEventListener('waiting', handleEvent);
    v.addEventListener('progress', handleEvent);

    _hh_log("event hook for video player added.");
}
function _ytb_iosInjectAdStyle() {
    if (document._hh_ytbStyleInjected) {
        return;
    }

    var adStyle = _hh_getAdStyle();
    var nodeStyle = `
    button[class='open-app-button'] { ${adStyle} !important; }
    lazy-list > ytm-promoted-sparkles-web-renderer { ${adStyle} !important; }
    ytm-watch-metadata-app-promo-renderer { ${adStyle} !important; }
    `;

    document._hh_ytbStyleInjected = true;
    var node = document.createElement('style');
    node.innerHTML = nodeStyle;
    document.body.appendChild(node);

    _hh_log("youtube AD style override injected!");
}

function _ytb_cleanAdForIOS() {

    _ytb_iosInjectAdStyle();

    _ytb_iosInterceptAdVideo();

    var player = document.querySelector("#movie_player");
    if (player != null) {
        var q = player.getAvailableQualityLevels();
        if (null != q && q.length > 0) {
            player.setPlaybackQualityRange(q[0], q[0]);
            _hh_log("ytb video quality set to " + q[0]);
        }
    }

    _hh_log("remove youtube AD done(iOS)!");
}

function remove_text_ad_android() {
    const ads = ['button[class="open-app-button"]',
        'lazy-list > ytm-promoted-sparkles-web-renderer',
        'ytm-watch-metadata-app-promo-renderer',
        'ytm-companion-slot'];

    ads.forEach(function (cnd) {
        var ele = document.querySelector(cnd);
        if (null != ele) {
            if (_hh_jsEnv.hideAd) {
                ele.remove();
                _hh_updAdCounter(false);
            } else {
                ele.style = `${_hh_getAdStyle()} !important;`;
                _hh_updAdCounter(false);
            }
        }
    })
}

function videoEvent(event) {
    let eventName = event.type;
    _hh_log('got player event: ' + eventName);
    let playing = "play" == eventName || "playing" == eventName;

    let player = document.querySelector(".html5-video-player");
    let video = player.querySelector('video');
    _hh_videoStatusChanged(playing, video.videoWidth, video.videoHeight);
//    if (playing && !player.isFullscreen) {
//        btn = document.querySelector("button[class='icon-button fullscreen-icon']");
//        if (btn) {
//            btn.click();
//        }
//    }
}

function remove_ad_android() {
    _hh_log("check running...");

    remove_text_ad_android();

    var player = document.querySelector(".html5-video-player");
    if (!player) {
        return;
    }

    var video = player.querySelector('video');
    if(!video) {
        return
    }

    video.addEventListener('play', videoEvent);
    video.addEventListener('playing', videoEvent);
    video.addEventListener('pause', videoEvent);
    video.addEventListener('abort', videoEvent);

    // setInterval(fullscreenForPIP, 1000);

    if(!player.classList.contains('ad-showing')) {
        return;
    }

    const btn = player.querySelector(".ytp-ad-skip-button");
    if(btn) {
        _hh_log('simulate SKIP-AD click');
        // btn.click();
        _hh_simulateClick(btn);
        _hh_updAdCounter(true);
    } else {
        _hh_log('try to reduce ad video play time...');
        video.currentTime = isNaN(video.duration) ? 0 : video.duration;
    }
}

function ytbVideoFullscreen() {
    _hh_log('try to set screen full...');
    var player = document.querySelector(".html5-video-player");
    if (!player) {
        return;
    }

    if (player.isFullscreen()) {
        return;
    }

    btn = document.querySelector("button[class='icon-button fullscreen-icon']")
    document.getElementById("player-control-overlay").classList.add("fadein")
    btn.click()
    document.getElementById("player-control-overlay").classList.remove("fadein")
}

function fullscreenForPIP() {
    var player = document.querySelector(".html5-video-player");

    if (!player.isFullscreen()) {
        btn = document.querySelector("button[class='icon-button fullscreen-icon']");
        if (btn) {
            btn.click();
        }
    }
}

function _watchWindowSize() {
	console.log("pip: window size changed: width: " + document.documentElement.clientWidth);
	var width = document.documentElement.clientWidth;
	var video = document.querySelector('video');
	var player = document.querySelector(".html5-video-player");
	var playerContainer = document.getElementById("player-container-id");
    playerContainer.classList.remove("sticky-player");

	console.log(`video size: ${video.videoWidth} * ${video.videoHeight}`);

	var rect = player.getBoundingClientRect();
	console.log(`player rect: ${rect.width} ${rect.height}`);
	var height = parseInt(width * video.videoHeight / video.videoWidth);

	var style = `width: ${width}px; height: ${height}px; left: 0px; top: 0px;`;
	console.log(style);

    // playerContainer.classList.add("sticky-player");
	// video.style = style;


    window.__hh_PIP_isBig = document.documentElement.clientWidth/window.screen.width > 0.5;
    var video = player.querySelector('video');
    var playerContainer = document.getElementById("player-container-id");
    var player = document.getElementById("player");

    [player, video].forEach(ele => {
        ele.style = style;
    })


    // player.toggleFullscreen();  // "Failed to execute 'requestFullscreen' on 'Element': API can only be initiated by a user gesture."
}

function ytbTogglePiP(pip) {
    _hh_log('ytbTogglePiP...... ' + pip);

    var player = document.querySelector(".html5-video-player");
    var header = document.querySelector("header");
    var video = player.querySelector('video');
    var playerContainer = document.getElementById("player-container-id");
    if (!header || !player || !video) {
        return;
    }

    player.hideControls();
    var height = header.getBoundingClientRect().height;

    if (pip) {
        pco = document.getElementById("player-control-overlay");
        if (pco) { // 弹出了控制层
            pco.classList.remove("fadein");
        }

        fc = document.querySelector("div[class='fullscreen-controls expanded']");
        if (fc) { // 弹出了控制层
            fc.classList.remove("expanded");
        }

        header.style.display = "none";
        playerContainer.classList.remove("sticky-player");
        window.scrollTo(0, height);
        _watchWindowSize();
        window.addEventListener("resize", _watchWindowSize);

        // video.requestFullscreen();  // "Failed to execute 'requestFullscreen' on 'Element': API can only be initiated by a user gesture."
    } else {
        header.style.display = "";
        playerContainer.classList.add("sticky-player");
        window.removeEventListener("resize", _watchWindowSize);

       var video = player.querySelector('video');
       var player = document.getElementById("player");
       var mplayer = document.querySelector(".html5-video-player");

       [player, video].forEach(ele => {
           ele.style = "";
       })

       // setTimeout(function() {mplayer.stopVideo();}, 100);
       // setTimeout(function() {mplayer.playVideo();}, 500);
    }
}

function _keepPlaying() {
    _hh_log('pip: _keepPlaying......');
    var mplayer = document.querySelector(".html5-video-player");
    if (mplayer) {
        setTimeout(function() {mplayer.pauseVideo();}, 100);
        setTimeout(function() {mplayer.playVideo();}, 300);
    }
}

function _forceStopPlaying() {
    _hh_log('pip: _forceStopPlaying......');
    var mplayer = document.querySelector(".html5-video-player");
    if (mplayer) {
        setTimeout(function() {mplayer.stopVideo();}, 600);
    }
}

function _fullAndPlay() {
    _hh_log('going to fullscreen......');

    var player = document.querySelector(".html5-video-player");

    if (!player.isFullscreen()) {
        btn = document.querySelector("button[class='icon-button fullscreen-icon']");
        if (btn) {
            btn.click();
        }
    }
}

function ytbToggleFullscreen() {
    _hh_log('try to set screen full...');
    var player = document.querySelector(".html5-video-player");
    if (!player) {
        return;
    }

    if (player.isFullscreen()) {
        return;
    }

    _hh_log('try to found screen button...');
    setTimeout(_fullAndPlay, 200);
}

function _ytb_cleanAdForAndroid() {
    remove_ad_android()

    var player = document.querySelector(".html5-video-player");
    // ['hd720', 'large', 'medium', 'small', 'tiny', 'auto']
    if (player != null) {
        var q = player.getAvailableQualityLevels();
        if (null != q && q.length > 0) {
//            if (q.includes('hd720')) {
//                player.setPlaybackQualityRange('hd720', 'hd720');
//                _hh_log("ytb video quality: hd720");
//            }
        }
    }
}
