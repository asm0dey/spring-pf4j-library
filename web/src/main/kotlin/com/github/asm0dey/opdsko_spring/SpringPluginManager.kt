package com.github.asm0dey.opdsko_spring

import jakarta.annotation.PostConstruct
import org.pf4j.DefaultPluginManager
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component
import java.nio.file.Path

@Component
class SpringPluginManager(@Value("\${pf4j.plugin-dir}") pluginDir: Path) :
    DefaultPluginManager(pluginDir),
    ApplicationContextAware {
    @Autowired
    lateinit var applicationContext_: ApplicationContext

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        applicationContext_ = applicationContext
    }

    /**
     * This method load, start plugins and inject extensions in Spring
     */
    @PostConstruct
    fun init() {
        loadPlugins()
        startPlugins()

        val beanFactory = applicationContext_.autowireCapableBeanFactory as AbstractAutowireCapableBeanFactory
        val extensionsInjector = ExtensionsInjector(this, beanFactory)
        extensionsInjector.injectExtensions()
    }

}

class ExtensionsInjector(
    private val springPluginManager: SpringPluginManager,
    private val beanFactory: AbstractAutowireCapableBeanFactory,
) {
    fun injectExtensions() {
        // add extensions from classpath (non plugin)
        var extensionClassNames = springPluginManager.getExtensionClassNames(null)
        for (extensionClassName in extensionClassNames) {
            try {
                log.debug("Register extension '{}' as bean", extensionClassName)
                val extensionClass = javaClass.classLoader.loadClass(extensionClassName)
                registerExtension(extensionClass)
            } catch (e: ClassNotFoundException) {
                log.error(e.message, e)
            }
        }

        // add extensions for each started plugin
        val startedPlugins = springPluginManager.startedPlugins
        for (plugin in startedPlugins) {
            log.debug(
                "Registering extensions of the plugin '{}' as beans",
                plugin.pluginId
            )
            extensionClassNames = springPluginManager.getExtensionClassNames(plugin.pluginId)
            for (extensionClassName in extensionClassNames) {
                try {
                    log.debug("Register extension '{}' as bean", extensionClassName)
                    val extensionClass = plugin.pluginClassLoader.loadClass(extensionClassName)
                    registerExtension(extensionClass)
                } catch (e: ClassNotFoundException) {
                    log.error(e.message, e)
                }
            }
        }
    }

    /**
     * Register an extension as bean.
     * Current implementation register extension as singleton using `beanFactory.registerSingleton()`.
     * The extension instance is created using `pluginManager.getExtensionFactory().create(extensionClass)`.
     * The bean name is the extension class name.
     * Override this method if you wish other register strategy.
     */
    protected fun registerExtension(extensionClass: Class<*>) {
        val extensionBeanMap = springPluginManager.applicationContext_.getBeansOfType(extensionClass)
        if (extensionBeanMap.isEmpty()) {
            val extension = springPluginManager.extensionFactory.create(extensionClass)
            beanFactory.registerSingleton(extensionClass.name, extension)
        } else {
            log.debug(
                "Bean registeration aborted! Extension '{}' already existed as bean!",
                extensionClass.name
            )
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ExtensionsInjector::class.java)
    }
}