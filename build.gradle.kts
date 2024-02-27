plugins {
    id("java")
}

group = "com.cjmckenzie"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.16.1")
    implementation("co.elastic.clients:elasticsearch-java:8.12.2")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    implementation("org.springframework.boot:spring-boot-starter-web:3.2.3")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.2.3")

    implementation("org.apache.commons:commons-lang3:3.14.0")
    implementation("com.thedeanda:lorem:2.2")
}

tasks.test {
    useJUnitPlatform()
}