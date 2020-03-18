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

rootProject.name = "worldedit-adapters"

data class Ver(val minor: Int, val rel: String)

fun vers(minor: Int, vararg rels: String) = rels.map { Ver(minor, it) }

listOf(
    vers(13, "1", "2", "2_2"),
    vers(14, "1", "2", "3", "4"),
    vers(15, "1", "2")
)
    .flatten()
    .forEach { include("spigot_v1_${it.minor}_R${it.rel}") }
