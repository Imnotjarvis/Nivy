/**
 * Precompiled [ivy.kotlin-android.gradle.kts][Ivy_kotlin_android_gradle] script plugin.
 *
 * @see Ivy_kotlin_android_gradle
 */
public
class Ivy_kotlinAndroidPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(target: org.gradle.api.Project) {
        try {
            Class
                .forName("Ivy_kotlin_android_gradle")
                .getDeclaredConstructor(org.gradle.api.Project::class.java, org.gradle.api.Project::class.java)
                .newInstance(target, target)
        } catch (e: java.lang.reflect.InvocationTargetException) {
            throw e.targetException
        }
    }
}
