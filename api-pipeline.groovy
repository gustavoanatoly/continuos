import groovy.json.JsonSlurperClassic
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder
import groovy.json.JsonParserType
import groovy.json.StringEscapeUtils


@NonCPS
def jsonParse(def json) {
    new groovy.json.JsonSlurperClassic().parseText(json)
}

def jsonSlurpLaxWithoutSerializationTroubles(String jsonText)
{
    return new JsonSlurperClassic().parseText(
        new JsonBuilder(
            new JsonSlurper()
                .setType(JsonParserType.LAX)
                .parseText(jsonText)
        )
        .toString()
    )
}
node ('master') {
    String swaggerSensedia
    stage('All') {
        git url:'https://github.com/gustavoanatoly/continuos.git', branch:"main"

        def gitSha
        def filesModified
        def pwd = pwd()
        def sensediaAuth = '777acf98-7f8e-3cd8-b55d-7871671b21d2'

        try {
            gitSha = sh (
                script: 'git rev-parse --short HEAD',
                returnStdout:true
            ).trim()
            
            filesModified = sh(
                script: 'git diff-tree --no-commit-id  --name-only -r ' + gitSha,
                returnStdout:true
            ).trim()

            def fileLines = filesModified.split("\n")
            println "Selecting json files to deploy"
            for (file in fileLines) {
                def (name, extension) = file.tokenize('.')
                
                if (extension == "json") {
                    println("Found: " + file + " adding to deploy stage")
                    def content = readJSON file: workspace + "/" + file
                    //println content
                    
                    def jsonSensedia = new groovy.json.JsonSlurper().parseText(
                            '''{
                                "name": "string",
                                "swagger": "string",
                                "version": "string"
                            }''')
                    jsonSensedia.name = name
                    jsonSensedia.swagger = JsonOutput.toJson(content)
                    jsonSensedia.version = "1.0"
                    println jsonSensedia.swagger
                    // swaggerSensedia = new JsonBuilder(jsonSensedia).toString()
                    swaggerSensedia = JsonOutput.toJson(jsonSensedia)
                    println swaggerSensedia



                    
                }
                    
            }
        
        } catch(Exception ex) {
            println("Erro ao tentar executar commandos do git: ${ex}")
        }
    }

    stage("Shell") {        
        writeFile file: "data.json", text: swaggerSensedia

        def cmd = 'curl -X POST \"https://manager-demov3.sensedia.com/api-manager/api/v3/apis/swagger\" -H \"accept: */*\" -H \"Sensedia-Auth: 5d259f89-cdb3-30ef-a9b4-8c6d61837912\" -H \"Content-Type: application/json\" -d @data.json'
        println cmd
        def response = sh script: cmd 
    }

}