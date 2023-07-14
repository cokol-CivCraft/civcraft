import java.io.File

File("./run/plugins").let { sourceFile ->
    if (sourceFile.isDirectory) {
        return@let
    }
    sourceFile.mkdir()
}
File("./lib/EssentialsX-2.20.0.jar").let { sourceFile ->
    if (File("./run/plugins/EssentialsX-2.20.0.jar").exists()) {
        return@let;
    }
    sourceFile.copyTo(File("./run/plugins/EssentialsX-2.20.0.jar"))
}
File("./lib/TitleAPI-1.8.1.jar").let { sourceFile ->
    if (File("./run/plugins/TitleAPI-1.8.1.jar").exists()) {
        return@let;
    }
    sourceFile.copyTo(File("./run/plugins/TitleAPI-1.8.1.jar"))
}
File("./lib/Vault.jar").let { sourceFile ->
    if (File("./run/plugins/Vault.jar").exists()) {
        return@let;
    }
    sourceFile.copyTo(File("./run/plugins/Vault.jar"))
}
File("./lib/WorldBorder.jar").let { sourceFile ->
    if (File("./run/plugins/WorldBorder.jar").exists()) {
        return@let;
    }
    sourceFile.copyTo(File("./run/plugins/WorldBorder.jar"))
}
File("./out/artifacts/civcraft_jar/civcraft.jar").let { sourceFile ->
    if (File("./run/plugins/civcraft.jar").exists()) {
        File("./run/plugins/civcraft.jar").delete()
    }
    sourceFile.copyTo(File("./run/plugins/civcraft.jar"))
}