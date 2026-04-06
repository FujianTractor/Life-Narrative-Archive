$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
$env:Path='C:\Program Files\Java\jdk-25.0.2\bin;' + $env:Path
$env:OPENAI_API_KEY='test-key'
$env:APP_JWT_SECRET='local-dev-secret'
& 'D:\github\Life-Narrative-Archive\.tools\apache-maven-3.9.9\bin\mvn.cmd' spring-boot:run