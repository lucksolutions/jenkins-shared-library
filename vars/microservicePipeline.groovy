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

            dir("${config.directory}") {

                stage('Maven Build') {
                    def mavenSettings = libraryResource 'com/lucksolutions/maven/settings.xml'
                    writeFile file: 'settings.xml', text: mavenSettings
                    withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                        sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -s settings.xml clean compile test-compile'
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

                stage('Code Analysis') {
                    withSonarQubeEnv('CI') {
                        sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -s settings.xml sonar:sonar'
                    }
                }

                stage("Quality Gate") {
                    timeout(time: 15, unit: 'MINUTES') {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                        }
                    }
                }

                stage('Deploy to Repository') {
                    withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                        sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -s settings.xml deploy'
                    }
                }

                stage("Build ${config.imageName}") {
                    docker.withServer('tcp://ip-10-247-80-40.us-gov-west-1.compute.internal:2375') {
                        docker.withRegistry('https://index.docker.io/v1/', 'dockerhub') {
                            def image = docker.build("${config.imageName}:${BRANCH_NAME}")
                        }
                    }
                }
                stage("Deploy ${config.imageName}") {
                    sh 'echo "Deploying Docker Container..."'
                }
                stage("Test ${config.imageName}") {
                    sh 'echo "Executing test cases..."'
                }
                stage("Push ${config.imageName} to Registry") {
                    docker.withServer('tcp://ip-10-247-80-40.us-gov-west-1.compute.internal:2375') {
                        docker.withRegistry('https://index.docker.io/v1/', 'dockerhub') {
                            def image = docker.build("${config.imageName}:${BRANCH_NAME}")
                            image.push()
                            if (env.BRANCH_NAME == 'development') {
                                image.push('latest')
                            }
                        }
                    }
                }
            }
        } finally {
            //Send build notifications if needed
            notifyBuild(currentBuild.result)
        }
    }

}