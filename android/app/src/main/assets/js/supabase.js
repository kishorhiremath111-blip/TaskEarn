"use strict";

/* =====================================================
   TASK EARN - SUPABASE CONNECTION
===================================================== */

const SUPABASE_URL =
    "https://wfvmipwesttejtvwoasj.supabase.co";

const SUPABASE_PUBLISHABLE_KEY =
    "sb_publishable_o0R1owOtbfN-YaOsR_MzRg_lgbbqbvV";


/* =====================================================
   CREATE CLIENT
===================================================== */

let supabaseClient = null;


if (
    typeof window !== "undefined" &&
    window.supabase &&
    typeof window.supabase.createClient === "function"
) {

    supabaseClient =
        window.supabase.createClient(
            SUPABASE_URL,
            SUPABASE_PUBLISHABLE_KEY,
            {
                auth: {
                    persistSession: true,
                    autoRefreshToken: true,
                    detectSessionInUrl: true
                }
            }
        );

    console.log(
        "TaskEarn: Supabase connected successfully."
    );

} else {

    console.error(
        "TaskEarn: Supabase library not loaded."
    );

}