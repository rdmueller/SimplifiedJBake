import groovy.util.ConfigSlurper

class ConfigLoader {

    static Map loadConfig(String configFilePath) {
        def configFile = new File(configFilePath)
        println configFile.canonicalPath
        def configSlurper = new ConfigSlurper()
        return configSlurper.parse(configFile.text)
    }
}