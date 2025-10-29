// ── mechanic.ink ────────────────────────────────────────────────
// Variables that the game will inject just before each talk()
VAR has_key        = false      // player owns the brass key?
VAR has_wrench     = false      // player owns the wrench?
VAR quest_finished = false      // quest “Return the Wrench” complete?

// Local bookkeeping
VAR visited = false             // has the player already talked once?

=== start ===
~ visited = false
~ has_key = VAR? has_key
~ has_wrench = VAR? has_wrench
~ quest_finished = VAR? quest_finished

{ quest_finished:
    -> after_all
- else:
    { has_wrench:
        -> prompt_give
    - else:
        { visited:
            -> hint_key
        - else:
            ~ visited = true
            -> need_wrench
        }
    }
}

=== need_wrench
"Could you bring me my heavy‑duty wrench from my bunk?"
-> END

=== hint_key
"Still no luck? The locker in the bedroom is locked—there’s a little brass key around here somewhere…"
-> END

=== prompt_give
"You’ve already got it? Well hand it on over!"
-> END

=== got_wrench            // you’ll jump here from the game code
"Thanks for the wrench! Here’s an access code—don’t lose it."
~ quest_finished = true
-> after_all

=== after_all
"Good to see you again. Need a tune‑up?"
-> END
