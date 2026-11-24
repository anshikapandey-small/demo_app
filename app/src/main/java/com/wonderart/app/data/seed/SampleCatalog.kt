package com.wonderart.app.data.seed

import androidx.annotation.DrawableRes
import com.example.R
import com.wonderart.app.data.model.Course

object SampleCatalog {

    const val COURSE_PENCIL_ID = "course_pencil_sketching"
    const val COURSE_WATERCOLOR_ID = "course_watercolor_masterclass"
    const val COURSE_PORTRAIT_ID = "course_portrait_realism"
    const val COURSE_ACRYLIC_ID = "course_acrylic_beginners"

    val sampleCourseSeeds: List<Pair<String, Map<String, Any>>> = listOf(
        COURSE_PENCIL_ID to mapOf(
            "title" to "Pencil Sketching Fundamentals",
            "description" to "Learn basic sketching, shading, proportions and pencil techniques.",
            "instructor" to "Nikhil",
            "thumbnailKey" to "eyes_expression",
            "category" to "Pencil Sketching",
            "price" to 499L,
            "difficulty" to "Beginner",
            "duration" to "4 weeks",
            "totalSessions" to 12L,
            "curriculum" to listOf(
                "Week 1: Graphite grades, grip & confident line control",
                "Week 2: Geometric shapes, perspective & measuring proportions",
                "Week 3: Value scales, hatching, cross-hatching & blending",
                "Week 4: Still life shading & light-shadow composition"
            ),
            "learningObjectives" to listOf(
                "Control graphite weights from 2H to 6B",
                "Construct accurate proportions using sighting methods",
                "Render smooth tonal gradients and cast shadows"
            ),
            "published" to true
        ),
        COURSE_WATERCOLOR_ID to mapOf(
            "title" to "Watercolor Painting Masterclass",
            "description" to "Explore colour mixing, watercolor techniques, landscapes and creative compositions.",
            "instructor" to "Nikhil",
            "thumbnailKey" to "silent_mountains",
            "category" to "Watercolor",
            "price" to 799L,
            "difficulty" to "Beginner to Intermediate",
            "duration" to "6 weeks",
            "totalSessions" to 18L,
            "curriculum" to listOf(
                "Week 1-2: Water-to-pigment control, wet-on-wet & flat washes",
                "Week 3-4: Colour harmony, layering glazes & foliage textures",
                "Week 5-6: Atmospheric mountain landscapes & reflections"
            ),
            "learningObjectives" to listOf(
                "Master wet-on-wet and wet-on-dry watercolor washes",
                "Mix luminous natural greens, skies, and shadow tones",
                "Paint multi-layered atmospheric landscapes"
            ),
            "published" to true
        ),
        COURSE_PORTRAIT_ID to mapOf(
            "title" to "Portrait Drawing and Realism",
            "description" to "Learn facial proportions, realistic shading, expressions and detailed portrait drawing.",
            "instructor" to "Nikhil",
            "thumbnailKey" to "course_portrait",
            "category" to "Portrait",
            "price" to 1299L,
            "difficulty" to "Intermediate",
            "duration" to "8 weeks",
            "totalSessions" to 24L,
            "curriculum" to listOf(
                "Week 1-2: Loomis head construction & facial planes",
                "Week 3-4: Detailed eye, nose, lip & ear anatomy",
                "Week 5-6: Skin tones, hair texture & subtle expressions",
                "Week 7-8: Full charcoal & graphite realism portrait"
            ),
            "learningObjectives" to listOf(
                "Map accurate head tilt and facial landmarks",
                "Capture lifelike eye highlights and facial expressions",
                "Build depth with charcoal and soft graphite transitions"
            ),
            "published" to true
        ),
        COURSE_ACRYLIC_ID to mapOf(
            "title" to "Acrylic Painting for Beginners",
            "description" to "Understand acrylic paints, brush techniques, colour theory and canvas painting.",
            "instructor" to "Nikhil",
            "thumbnailKey" to "sunset_ocean",
            "category" to "Acrylic Painting",
            "price" to 699L,
            "difficulty" to "Beginner",
            "duration" to "4 weeks",
            "totalSessions" to 12L,
            "curriculum" to listOf(
                "Week 1: Canvas priming, brushes & acrylic mediums",
                "Week 2: Colour wheel, warm/cool contrasts & blending",
                "Week 3: Impasto palette knife strokes & sky gradients",
                "Week 4: Sunset seascape painting from sketch to varnish"
            ),
            "learningObjectives" to listOf(
                "Blend vibrant acrylic gradients before paint dries",
                "Use flat, filbert, and palette knife textures",
                "Complete a framed sunset canvas painting"
            ),
            "published" to true
        )
    )

    val demoCommunitySeeds: List<Pair<String, Map<String, Any>>> = listOf(
        "demo_post_silent_mountains" to mapOf(
            "userId" to "demo_aarav_sharma",
            "authorName" to "Aarav Sharma",
            "title" to "The Silent Mountains",
            "description" to "My first attempt at painting a peaceful mountain landscape using watercolors.",
            "category" to "Watercolor",
            "drawableKey" to "silent_mountains",
            "likesCount" to 24L,
            "commentsCount" to 2L,
            "demo" to true,
            "hidden" to false,
            "flagged" to false
        ),
        "demo_post_eyes_expression" to mapOf(
            "userId" to "demo_ananya_patil",
            "authorName" to "Ananya Patil",
            "title" to "Eyes of Expression",
            "description" to "Practicing realistic eyes and shading techniques.",
            "category" to "Pencil Sketching",
            "drawableKey" to "eyes_expression",
            "likesCount" to 38L,
            "commentsCount" to 2L,
            "demo" to true,
            "hidden" to false,
            "flagged" to false
        ),
        "demo_post_sunset_ocean" to mapOf(
            "userId" to "demo_riya_mehta",
            "authorName" to "Riya Mehta",
            "title" to "Sunset by the Ocean",
            "description" to "Experimenting with warm colours and sunset reflections.",
            "category" to "Acrylic Painting",
            "drawableKey" to "sunset_ocean",
            "likesCount" to 31L,
            "commentsCount" to 1L,
            "demo" to true,
            "hidden" to false,
            "flagged" to false
        ),
        "demo_post_floral_mandala" to mapOf(
            "userId" to "demo_aditya_deshmukh",
            "authorName" to "Aditya Deshmukh",
            "title" to "Floral Mandala",
            "description" to "A detailed mandala inspired by floral patterns and symmetry.",
            "category" to "Mandala Art",
            "drawableKey" to "floral_mandala",
            "likesCount" to 19L,
            "commentsCount" to 1L,
            "demo" to true,
            "hidden" to false,
            "flagged" to false
        )
    )

    val previewFallbackCourses: List<Course> = sampleCourseSeeds.map { (id, map) ->
        Course(
            id = id,
            title = map["title"] as String,
            description = map["description"] as String,
            instructor = map["instructor"] as String,
            thumbnailKey = map["thumbnailKey"] as String,
            category = map["category"] as String,
            price = map["price"] as Long,
            difficulty = map["difficulty"] as String,
            duration = map["duration"] as String,
            totalSessions = map["totalSessions"] as Long,
            curriculum = @Suppress("UNCHECKED_CAST") (map["curriculum"] as List<String>),
            learningObjectives = @Suppress("UNCHECKED_CAST") (map["learningObjectives"] as List<String>),
            published = true
        )
    }

    @DrawableRes
    fun resolveArtworkDrawable(key: String?): Int? = when (key) {
        "silent_mountains" -> R.drawable.img_art_silent_mountains
        "eyes_expression" -> R.drawable.img_art_eyes_expression
        "sunset_ocean" -> R.drawable.img_art_sunset_ocean
        "floral_mandala" -> R.drawable.img_art_floral_mandala
        "course_portrait" -> R.drawable.img_course_portrait
        else -> null
    }
}
