
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

for (jarTask in listOf(tasks.jar, tasks.sourcesJar)) {
    jarTask.configure {
        manifest {
            attributes("Lwjgl3ify-Aware" to true)
        }
    }
}
