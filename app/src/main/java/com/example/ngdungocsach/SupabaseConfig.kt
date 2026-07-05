package com.example.ngdungocsach

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage

object SupabaseConfig {
    // URL của dự án Supabase (không bao gồm /rest/v1/)
    const val SUPABASE_URL = "https://jasiqecndhvgtcvvfuoj.supabase.co"
    // Anon Key
    const val SUPABASE_KEY = "sb_publishable_o4rVypdF48CDxwn344RNRw_OulqBfcs"

    val client = createSupabaseClient(
        supabaseUrl = SUPABASE_URL,
        supabaseKey = SUPABASE_KEY
    ) {
        install(Storage)
    }

    const val BUCKET_NAME = "books-storage"

    fun getPublicUrl(fileName: String): String {
        return "$SUPABASE_URL/storage/v1/object/public/$BUCKET_NAME/$fileName"
    }
}
