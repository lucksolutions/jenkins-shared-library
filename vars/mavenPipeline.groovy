def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    pipeline {
        agent any

        triggers {
            //Check SCM every 5 minutes
            pollSCM('*/5 * * * *')
        }

        tools {
            maven 'Maven'
        }
        
        stages {
            stage('Maven Build') {
                steps {
                    dir("${config.directory}") {
                        sh 'mvn clean'
                        script {
                            def mavenSettings = libraryResource 'com/lucksolutions/maven/settings.xml'
                            writeFile('target/settings.xml', mavenSettings)
                        }
                        withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                            sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -s target/settings.xml deploy'
                        }
                    }
                }
            }
        }
    }
}