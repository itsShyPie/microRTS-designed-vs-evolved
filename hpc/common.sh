SEP=":"
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) SEP=";" ;; esac
CP="bin${SEP}lib/*${SEP}lib/bots/*"
JAVA_OPTS=(-Djava.awt.headless=true -Dexp.engineLog="${ENGINE_LOG:-false}")

command -v java >/dev/null || { echo "java not found: load your cluster's Java module first (Java 11 or newer)" >&2; exit 1; }
