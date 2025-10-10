dependencies {

    compileOnly("org.springframework.boot:spring-boot-starter-web")

    implementation(project(":domain"))
    implementation("com.auth0:java-jwt:4.4.0")

    testImplementation("org.springframework.boot:spring-boot-starter-web")
}
