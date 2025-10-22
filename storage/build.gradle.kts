dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("software.amazon.awssdk:s3:2.35.1")
    implementation("com.fasterxml.jackson.core:jackson-databind")

    runtimeOnly("com.mysql:mysql-connector-j")
    runtimeOnly("com.h2database:h2")

    testImplementation(project(":domain"))
    testImplementation(project(":auth"))

    compileOnly(project(":domain"))
    compileOnly(project(":auth"))

}
