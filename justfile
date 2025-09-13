default:
    @just --list

# Run the app from source
run:
    clj -M -m ankimigo.main

# Run cljfmt fix
fmt:
    clj -M:dev -m cljfmt.main fix deps.edn src/
