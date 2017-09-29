def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = "."
    }

    node {
        stage ('Setup') {
            dir('${config.directory') {
                sh 'mvn clean'
                def settings = libraryResource 'com/lucksolutions/maven/settings.xml'
                writeFile('target/settings.xml',  settings)
            }
        }
        stage('Build') {
            dir('${config.directory') {
                withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                    sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -s target/settings.xml deploy'
                }
            }
        }
        stage ('Tests') {
            parallel 'static': {
                sh "echo 'shell scripts to run static tests...'"
            },
            'unit': {
                sh "echo 'shell scripts to run unit tests...'"
            },
            'integration': {
                sh "echo 'shell scripts to run integration tests...'"
            }
        }
        stage ('Deploy') {
            sh "echo 'deploying to server ${config.serverDomain}...'"
        }
    }
}