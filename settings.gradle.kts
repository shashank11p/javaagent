rootProject.name = "traceable-otel-agent"

include(":javaagent")
include("instrumentation")
include("instrumentation:servlet:servlet-3.0")
findProject(":instrumentation:servlet:servlet-3.0")?.name = "servlet-3.0"