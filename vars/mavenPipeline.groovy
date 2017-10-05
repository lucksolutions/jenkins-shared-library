def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    node {
        properties([
            pipelineTriggers([
                pollSCM('*/5 * * * *')
            ])
        ])

        stage('Checkout SCM') {
            checkout scm
        }
        stage('Maven Build') {
            dir("${config.directory}") {
                try {
                    def mavenSettings = libraryResource 'com/lucksolutions/maven/settings.xml'
                    writeFile file: 'settings.xml', text: mavenSettings
                    withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                        sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -s settings.xml clean deploy'
                    }
                }
                finally {
                    echo 'Publishing Test Reports...'
                    step([$class: 'JUnitResultArchiver', testResults: '**/surefire-reports/*.xml', healthScaleFactor: 1.0, allowEmptyResults: true])
                    publishHTML (target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: false,
                        keepAll: true,
                        reportDir: 'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: "Code Coverage"
                    ])
                }
            }
        }
    }

}