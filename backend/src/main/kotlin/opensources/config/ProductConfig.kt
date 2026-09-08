package opensources.config

data class ProductConfig(
    val name: String,
    val description: String,
    val logo: String,
    val defaultLocale: String,
    val defaultTimezone: String,
    val supportEmail: String,
    val applicationUrl: String,
    val enabledModules: Set<String>,
) {
    fun isModuleEnabled(module: String): Boolean = module in enabledModules
}
