/*
 * WorldEdit, a Minecraft world manipulation toolkit
 * Copyright (C) sk89q <http://www.sk89q.com>
 * Copyright (C) WorldEdit team and contributors
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
plugins {
    java
}

subprojects {
    apply(plugin = "java")
    group = "com.sk89q.worldedit.adapters"
    version = "1.0"

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(16))
    }

    repositories {
        mavenLocal()
        mavenCentral()
        maven { url = uri("https://maven.enginehub.org/repo/") }
    }

    dependencies {
        implementation("com.sk89q.worldedit:worldedit-bukkit:7.3.0-SNAPSHOT")
    }

    tasks.compileJava.configure {
        options.release.set(8)
    }

    configurations.all {
        attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 16)
    }
}

mapOf(
    "spigot_v1_13_R2_2" to "1.13.2",
    "spigot_v1_14_R4" to "1.14.4",
    "spigot_v1_15_R2" to "1.15.2",
    "spigot_v1_16_R3" to "1.16.5",
    "spigot_v1_17_R1" to "1.17",
    "spigot_v1_17_R1_2" to "1.17.1",
).forEach { (projectName, ver) ->
    project(":$projectName") {
        dependencies.implementation("org.spigotmc", "spigot", "${ver}-R0.1-SNAPSHOT")
    }
}

tasks.jar {
    from(subprojects.map {
        it.sourceSets["main"].output
    })
}
