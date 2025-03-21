package inhouse.ad

import com.google.gson.Gson


fun mockId(mockId: Int) : String {
    return "mock_ad_$mockId"
}

fun AdEntry.mockTitle(mockId:Int) : String {
    return "mock_ad_${mockId}_${adAction}_${mediaType}"
}

fun AdEntry.mockAssetUrl(mockId:Int) : String {
    return when (mediaType) {
        AdMediaType.IMAGE -> {
            "https://dummyimage.com/800x480/e31e25/ffffff&text=mock_ad_item_$mockId"
        }
        AdMediaType.HTML -> {
            "https://google.com/search?q=Game+$mockId"
        }
        AdMediaType.VIDEO -> {
            "https://dsr15f7k4jp2t.cloudfront.net/video_ad_$mockId.mp4"
        }
    }
}

fun AdEntry.mockAdActionUrl(mockId:Int) : String {
    return when (adAction) {
        AdActionType.StoreDeepLink -> {
            "amzn://apps/android?s=Game+$mockId"
        }
        AdActionType.WebLink -> {
            "https://google.com/search?q=Game+$mockId"
        }
    }
}

fun AdEntry.enableDubugItemForImageAd() {
    this.adAction = AdActionType.StoreDeepLink
    this.mediaType = AdMediaType.IMAGE
    this.adItemId = "local_debug_ad_item_for_image_ad"
    this.adTitle = "DEBUG MODE: IMAGE AD"
    this.adActionUrl = "amzn://apps/android?s=Game"
    this.assetUrl = "/sdcard/simple_ad_v1_debug_image.png"
    this.debugMode = true
}

fun AdEntry.enableDubugItemForVideoAd() {
    this.adAction = AdActionType.StoreDeepLink
    this.mediaType = AdMediaType.VIDEO
    this.adItemId = "local_debug_ad_item_for_video_ad"
    this.adTitle = "DEBUG MODE: VIDEO AD"
    this.adActionUrl = "amzn://apps/android?s=Game"
    this.assetUrl = "/sdcard/simple_ad_v1_debug_video.mp4"
    this.debugMode = true
}

fun AdEntry.setMockValues(i:Int) {
    val mod = i % 3
    var t:AdMediaType
    var a:AdActionType
    when (mod) {
        1 ->  {
            t = AdMediaType.VIDEO
            a = AdActionType.StoreDeepLink
        }
        2 -> {
            t = AdMediaType.HTML
            a = AdActionType.WebLink
        }
        else -> {
            t = AdMediaType.IMAGE
            a = AdActionType.StoreDeepLink
        }
    }
    this.adAction = a
    this.mediaType = t
    this.adItemId = mockId(i)
    this.adTitle = this.mockTitle(i)
    this.adActionUrl = this.mockAdActionUrl(i)
    this.assetUrl = this.mockAssetUrl(i)
}


fun main() {
    println()

    val db = SimpleAdDb()
    db.ts = System.currentTimeMillis()

    for (i in 1..10) {
        val e = AdEntry()
        e.setMockValues(i)
        db.adEntries.add(e)
    }

    val gson = Gson()
    val s = gson.toJson(db)
    println(s)
    println()

}