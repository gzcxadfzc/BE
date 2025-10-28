dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    implementation(project(":auth"))
    implementation(project(":domain"))
    implementation(project(":core"))

    runtimeOnly(project(":storage"))
    runtimeOnly(project(":ai"))

    testImplementation(project(":storage"))
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis")
    testImplementation("org.junit.jupiter:junit-jupiter")
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
    from(project(":ai").file("src/main/resources"))

    into(file("$projectDir/build/resources/main"))

    include("*.yml")
}

