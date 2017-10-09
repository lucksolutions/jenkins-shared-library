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

        try {

            stage('Checkout SCM') {
                checkout scm
            }
            stage('Maven Build') {
                dir("${config.directory}") {
                    def mavenSettings = libraryResource 'com/lucksolutions/maven/settings.xml'
                    writeFile file: 'settings.xml', text: mavenSettings
                    withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                        sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -s settings.xml clean compile test-compile'
                    }
                }
            }

            try {
                stage('Unit Testing') {
                    sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -Dmaven.test.failure.ignore=true -s settings.xml test'   
                }
            } finally {
                step([$class: 'JUnitResultArchiver', testResults: '**/surefire-reports/*.xml', healthScaleFactor: 1.0, allowEmptyResults: true])
                if (currentBuild.result == 'UNSTABLE') {
                    return 
                }
            }
            stage('Package') {
                try {
                    sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -s settings.xml package'
                } finally {
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

            stage('Deploy to Repository') {
                withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                    sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -s settings.xml deploy'
                }
            }
        } finally {
            //Send build notifications if needed
            notifyBuild(currentBuild.result)
        }
    }

}