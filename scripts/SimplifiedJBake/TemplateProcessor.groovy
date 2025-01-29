import groovy.text.GStringTemplateEngine
import groovy.text.SimpleTemplateEngine
import java.util.logging.Level

class TemplateProcessor {

    static String processWithTemplate( Map attributes, Map binding, List renderedPages, Map config) {
        def templatePath = 'src/site/templates/page_toc.gsp'
        def templateContent = new File(templatePath).text
        templateContent = expandIncludes(templateContent)

        def engine = new SimpleTemplateEngine()

        //engine.setVerbose(false)
        def template
        try {
            template = engine.createTemplate(templateContent)
                             .make(binding).toString()
        } catch (Exception e) {
            // in case of an exception, ensure that
            // compiled template is output
            //engine.setVerbose(true)
            //template = engine.createTemplate(templateContent)
            //                 .make(binding).toString()
            SimplifiedJBake.logger.log(Level.SEVERE, e.toString())
        }
        return template
    }

    static String expandIncludes(String content, int step = 0) {
        def pattern = /<%\s*include\s*"(.+?)"\s*%>/
        def matcher = content =~ pattern
        def baseDir = new File('src/site/templates')

        def contentIncluded = content
        matcher.each { fullMatch, fileName ->
            File includeFile = new File(baseDir, fileName)
            if (!includeFile.exists()) {
                SimplifiedJBake.logger.log(Level.SEVERE, "Include-File $fileName not found.")
            } else {
                String fileContent = "<!-- $fileName -->\n" + includeFile.text
                contentIncluded = contentIncluded.replace(fullMatch, fileContent)
            }
        }
        content = contentIncluded
        if (content.contains("include")) {
            if (step <= 10) {
                content = expandIncludes(content, step + 1)
            }
        }
        return content
    }
}