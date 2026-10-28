/**
 * Precompiled [ivy.integration.testing.gradle.kts][Ivy_integration_testing_gradle] script plugin.
 *
 * @see Ivy_integration_testing_gradle
 */
public
class Ivy_integration_testingPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Ivy_integration_testing_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
