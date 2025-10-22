dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation(project(":auth"))
    implementation(project(":domain"))
    implementation(project(":storage"))
    implementation(project(":core"))

    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

}


tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
}

tasks.named<Jar>("jar") {
    enabled = false
}

tasks.register<Copy>("copyResourcesConfig") {
    from(project(":auth").file("src/main/resources"))
    from(project(":storage").file("src/main/resources"))
    from(project(":core").file("src/main/resources"))

    into(file("$projectDir/build/resources/main"))

    include("*.yml")
}

