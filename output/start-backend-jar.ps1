$env:JAVA_HOME='C:\Program Files\Java\jdk-25.0.2'
$env:Path='C:\Program Files\Java\jdk-25.0.2\bin;' + $env:Path
$env:OPENAI_API_KEY='test-key'
$env:APP_JWT_SECRET='local-dev-secret'
& 'C:\Program Files\Java\jdk-25.0.2\bin\java.exe' -jar 'D:\github\Life-Narrative-Archive\backend\target\life-narrative-archive-backend-0.1.0-SNAPSHOT.jar'