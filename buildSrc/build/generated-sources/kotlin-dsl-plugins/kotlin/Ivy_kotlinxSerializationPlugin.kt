/**
 * Precompiled [ivy.kotlinx-serialization.gradle.kts][Ivy_kotlinx_serialization_gradle] script plugin.
 *
 * @see Ivy_kotlinx_serialization_gradle
 */
public
class Ivy_kotlinxSerializationPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Ivy_kotlinx_serialization_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
