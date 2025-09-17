default:
    @just --list

# Run the app from source
run:
    clj -M -m ankimigo.main

# Run cljfmt fix
fmt:
    clj -M:dev -m cljfmt.main fix deps.edn src/

# Run clj-kondo linter
lint:
    clj -M:dev -m clj-kondo.main --lint src
