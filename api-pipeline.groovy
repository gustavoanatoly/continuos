import groovy.json.JsonSlurperClassic
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import groovy.json.JsonBuilder
import groovy.json.JsonParserType
import groovy.json.StringEscapeUtils
import net.sf.json.JSONObject


@NonCPS
def jsonParse(def json) {
}

node ('master') {
    String swaggerSensedia
    String apiId // Store id of api expose
    String apiRevisionId // Store revision of api exposed
    String sensediaAuth = '5d259f89-cdb3-30ef-a9b4-8c6d61837912'
    stage('Preparation') {
        git url:'https://github.com/gustavoanatoly/continuos.git', branch:"main"

        def gitSha
        def filesModified
        def pwd = pwd()

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
                    
                    def jsonSensedia = 
                        JSONObject.fromObject('{"name": "string", "swagger": "string", "version": "string"}')
                    jsonSensedia.name = name
                    jsonSensedia.swagger = content.toString()
                    jsonSensedia.version = "1.0"
                    println jsonSensedia.swagger
                    // swaggerSensedia = new JsonBuilder(jsonSensedia).toString()
                    swaggerSensedia = jsonSensedia
                    println swaggerSensedia



                    
                }
                    
            }
        
        } catch(Exception ex) {
            println("Erro ao tentar executar commandos do git: ${ex}")
        }
    }

    stage("Exposicao") {
            
        writeFile file: "data.json", text: swaggerSensedia

        def cmd = 'curl -X POST \"https://manager-demov3.sensedia.com/api-manager/api/v3/apis/swagger\" -H \"accept: */*\" -H \"Sensedia-Auth: ' + sensediaAuth + '\" -H \"Content-Type: application/json\" -d @data.json'
        println cmd
        def response = sh(script: cmd, returnStdout: true).trim()
        println response
        def jsonResponse = new groovy.json.JsonSlurper().parseText(response)
        if (jsonResponse.id == null) {
            throw new Exception("Erro: " + jsonResponse)
        } else {
            apiId = jsonResponse.id
            apiRevisionId = jsonResponse.revisionId
        }
    }


    stage ("Deploy") {
        println "Deploy: Id: " + apiId + " revision: " + apiRevisionId
    }

}