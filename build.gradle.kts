plugins {
    java
    application
}

group = "com.dtsx.docs"
version = "1.0.0-alpha.1"

repositories {
    mavenCentral()
}

dependencies {
    // snapshot testing
    implementation("com.approvaltests:approvaltests:25.0.23")

    // test settings from .env files
    implementation("io.github.cdimascio:dotenv-java:3.2.0")

    // yaml
    implementation("tools.jackson.dataformat:jackson-dataformat-yaml:3.0.3")
    implementation("tools.jackson.core:jackson-databind:3.0.3")

    // static utils
    compileOnly("org.jetbrains:annotations:26.0.2")
    compileOnly("org.projectlombok:lombok:1.18.42")
    annotationProcessor("org.projectlombok:lombok:1.18.42")

    // no idea what this is :)
    implementation("com.datastax.astra:astra-db-java:2.1.4")
}

application {
    mainClass.set("com.dtsx.docs.Verifier")
}
