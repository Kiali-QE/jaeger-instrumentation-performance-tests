# Instructions on how to create the no-tracing branch
As a baseline we like to run the Jaeger Performance tests without tracing being turned on.  The best way to do this
is in a branch which has no Jaeger or OpenTracing dependencies defined in poms, and therefore no Jaeger or OpenTracing
code.

One way to do this would be to always have a **no-tracing** branch, and to cherry-pick traces.  This has proved to be 
problematic, so I think the easier way to do this is to periodically delete and the re-created the **no-tracing** branch, 
and then rip out all tracing related code.

## Delete the no-tracing branch

+ `git checkout master`
+ `git pull`
+ `git push origin --delete no-tracing`
+ `git branch -D no-tracing`

## Recreate the branch

+ git checkout -b no-tracing

## Remove dependencies from pom.xml files
+ pom.xml: remove dependencyManagement section with Jaeger Dependencies
+ All other poms: Remove all jaeger and opentracing dependencies

## Update source code
Remove all jaeger and opentracing imports and code from all java files.  

## Commit
git push origin no-tracing

## Example 
See this commit: https://github.com/Hawkular-QE/jaeger-instrumentation-performance-tests/commit/705b4d94fbba83d92bef4057249d61b0ac1d7888

