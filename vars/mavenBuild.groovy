def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }


    dir("${config.directory}") {
        stage('Debug') {
            echo "Branch Name: ${env.BRANCH_NAME}"
            echo "Change ID: ${env.CHANGE_ID}"
            echo "Change URL: ${env.CHANGE_URL}"
            echo "Change Target: ${env.CHANGE_TARGET}"
            echo "ChangeSet Size: ${currentBuild.changeSets.size()}"
        }

        stage('Maven Build') {
            def mavenSettings = libraryResource 'com/lucksolutions/maven/settings.xml'
            writeFile file: 'settings.xml', text: mavenSettings
            withCredentials([usernamePassword(credentialsId: 'nexus', usernameVariable: 'DEPLOY_USER', passwordVariable: 'DEPLOY_PASSWORD')]) {
                sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -DskipITs=true -s settings.xml clean compile test-compile'
            }
        }

        try {
            stage('Unit Testing') {
                sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -DskipITs=true -Dmaven.test.failure.ignore=true -s settings.xml test'   
            }
        } finally {
            step([$class: 'JUnitResultArchiver', testResults: '**/surefire-reports/*.xml', healthScaleFactor: 1.0, allowEmptyResults: true])
            if (currentBuild.result == 'UNSTABLE') {
                return 
            }
        }

        stage('Package') {
            try {
                sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -DskipITs=true -s settings.xml package'
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
            //See https://stackoverflow.com/questions/41695530/how-to-get-pull-request-id-from-jenkins-pipeline for details on how to "preview" sonar for PRs
            withSonarQubeEnv('CI') {
                sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -DskipITs=true -s settings.xml sonar:sonar'
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
                sh 'mvn -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true -Ddockerfile.skip=true -DskipITs=true -s settings.xml deploy'
            }
        }
    }
}