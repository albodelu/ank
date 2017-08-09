tasks {

    val plugin by creating(GradleBuild::class) {
        dir = file("plugin")
        tasks = listOf("publish")
    }

    val consumer by creating(GradleBuild::class) {
        dir = file("consumer")
        tasks = listOf("myEchoTask")
    }

    consumer.dependsOn(plugin)
}
