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
        
        stages {
            stage('Build Ascent Parent POM') {
                tools {
                    maven 'Maven'
                }
                steps {
                    dir("${config.directory}") {
                        withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                            sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -s ../settings.xml clean deploy'
                        }
                    }
                }
            }
        }
    }
}