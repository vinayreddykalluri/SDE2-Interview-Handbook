plugins {
    application
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass = "com.example.buildlab.Main"
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 21
    options.encoding = "UTF-8"
}
