// Zeke – simple greeting + recruitable flag
// ---------------------------------------------------------------

VAR recruited = false        // your game sets this to true when Zeke joins

=== start
{ recruited:
    -> after_all
- else:
    -> greet
}

=== greet
"Hey Nova, what's up?"
-> DONE

=== after_all
"All set—let's roll!"
-> DONE
