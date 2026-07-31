package com.collagex.app.collage

import com.collagex.app.data.CaptionMood

/**
 * Rule-based, fully offline caption + hashtag generator. It's a curated template
 * bank, not an LLM call (see README roadmap for a cloud AI-credits tier) — but it
 * runs instantly with zero network dependency and never returns an empty result.
 */
object CaptionGenerator {

    private val captions: Map<CaptionMood, List<String>> = mapOf(
        CaptionMood.TRAVEL to listOf(
            "Collecting moments, not things.",
            "Wander often, wonder always.",
            "New city, new memories.",
            "Somewhere between here and there.",
            "Passport full, heart fuller.",
        ),
        CaptionMood.BIRTHDAY to listOf(
            "Another year, another glow up.",
            "Cake, candles, and chaos — my favorite combo.",
            "Here's to the best year yet.",
            "Older, wiser, still iconic.",
            "Celebrating the main character energy.",
        ),
        CaptionMood.GYM to listOf(
            "Sweat now, shine later.",
            "Discipline over motivation.",
            "One more rep than yesterday.",
            "Building the body, building the mind.",
            "Progress, not perfection.",
        ),
        CaptionMood.CAFE to listOf(
            "But first, coffee.",
            "Slow mornings and warm cups.",
            "Latte art and good thoughts.",
            "Brewing up something good.",
            "Cafe hopping, one cup at a time.",
        ),
        CaptionMood.LOVE to listOf(
            "Found my favorite person.",
            "Every love story is beautiful, but ours is my favorite.",
            "Two hearts, one story.",
            "Lucky to call you mine.",
            "Home is wherever you are.",
        ),
        CaptionMood.FRIENDS to listOf(
            "Good times and crazy friends.",
            "Found my people.",
            "Friends who make memories together, stay together.",
            "Chosen family.",
            "The best therapy is friends and laughter.",
        ),
        CaptionMood.GRADUATION to listOf(
            "Proof that hard work pays off.",
            "New chapter, same ambition.",
            "Tassel turned, world awaits.",
            "Grateful for the journey, excited for what's next.",
            "Officially a graduate — let's go.",
        ),
    )

    private val hashtags: Map<CaptionMood, List<String>> = mapOf(
        CaptionMood.TRAVEL to listOf("#travelgram", "#wanderlust", "#travelphotography", "#explorepage", "#traveldiaries", "#instatravel", "#passportready", "#adventureawaits"),
        CaptionMood.BIRTHDAY to listOf("#birthdaygirl", "#birthdayboy", "#bdaymood", "#anotheryearolder", "#birthdayvibes", "#cakeandcandles", "#itsmybirthday", "#partytime"),
        CaptionMood.GYM to listOf("#gymlife", "#fitfam", "#nopainnogain", "#gymmotivation", "#fitnessjourney", "#trainhard", "#gymrat", "#strongnotskinny"),
        CaptionMood.CAFE to listOf("#coffeetime", "#cafevibes", "#coffeelover", "#latteart", "#brunchgoals", "#coffeeaddict", "#cafehopping", "#coffeeholic"),
        CaptionMood.LOVE to listOf("#couplegoals", "#inlove", "#soulmate", "#foreveryours", "#relationshipgoals", "#lovestory", "#mylove", "#togetherforever"),
        CaptionMood.FRIENDS to listOf("#squadgoals", "#bestfriends", "#friendsforever", "#friendship", "#girlgang", "#crew", "#memoriesmade", "#friendsquad"),
        CaptionMood.GRADUATION to listOf("#classof2026", "#gradlife", "#graduationday", "#proudgrad", "#gradsquad", "#newbeginnings", "#degreedone", "#graduated"),
    )

    fun captionsFor(mood: CaptionMood, count: Int = 3): List<String> =
        captions[mood].orEmpty().take(count)

    fun hashtagsFor(mood: CaptionMood, count: Int = 8): List<String> =
        hashtags[mood].orEmpty().take(count)
}
