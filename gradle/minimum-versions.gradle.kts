// Raises libraries the build tools pull in (the Gradle plugins, lint, the SDK libraries, test
// runners) to the minimum versions in the version catalog's build-tooling-minimums bundle: older
// versions have known vulnerabilities. It only raises: a dependency that already asks for a newer
// version keeps it, and Guava keeps the flavour it asked for (-jre or -android).
// Applied by the root build (every project) and by buildSrc (the plugins' own classpath).

val minimums = extensions.getByType<VersionCatalogsExtension>().named("libs")
    .findBundle("build-tooling-minimums").get().get()
    .associate { "${it.module.group}:${it.module.name}" to it.versionConstraint.requiredVersion }

fun numbers(version: String) =
    Regex("\\d+").findAll(version.substringBefore('-')).map { it.value.toInt() }.toList()

// False for an empty version, a range or a dynamic version, which this can't compare.
fun isOlder(version: String, minimum: String): Boolean {
    if (version.isEmpty() || version.any { it in "[](),+" }) return false
    val (have, need) = numbers(version) to numbers(minimum)
    val size = maxOf(have.size, need.size)
    fun padded(parts: List<Int>) = parts + List(size - parts.size) { 0 }
    val difference = padded(have).zip(padded(need)).firstOrNull { (a, b) -> a != b }
    return difference != null && difference.first < difference.second
}

allprojects {
    configurations.configureEach {
        resolutionStrategy.eachDependency {
            val minimum = minimums["${requested.group}:${requested.name}"] ?: return@eachDependency
            val version = requested.version.orEmpty()
            if (isOlder(version, minimum)) {
                useVersion(
                    if (version.endsWith("-android")) minimum.replace("-jre", "-android") else minimum
                )
                because("older versions have known vulnerabilities")
            }
        }
    }
}
