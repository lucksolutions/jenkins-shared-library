def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    stage('Maven Build') {
        tools {
            maven 'Maven'
        }
        steps {
            dir("${config.directory}") {
                script {
                    def mavenSettings = libraryResource 'com/lucksolutions/maven/settings.xml'
                    writeFile file: 'settings.xml', text: mavenSettings
                }
                withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                    sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -s settings.xml clean deploy'
                }
            }
        }
    }

}