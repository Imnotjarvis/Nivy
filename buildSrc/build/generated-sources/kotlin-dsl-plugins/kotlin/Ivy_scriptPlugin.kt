/**
 * Precompiled [ivy.script.gradle.kts][Ivy_script_gradle] script plugin.
 *
 * @see Ivy_script_gradle
 */
public
class Ivy_scriptPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Ivy_script_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
