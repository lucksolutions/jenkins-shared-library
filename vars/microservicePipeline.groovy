def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    if (config.dockerBuilds == null) {
        config.dockerBuilds = [
                "${config.imageName}": config.directory
        ]
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

                mavenBuild {
                    directory = config.directory
                }

                def keys = config.dockerBuilds.keySet();
                def builds = [:]
                for (int i=0; i<keys.size(); ++i) {
                    builds["echo ${keys[i]}": dockerBuild {
                        directory = config.dockerBuilds[keys[i]]
                        imageName = keys[i]
                    }]
                }
                sh "echo ${builds}"

                parallel builds
            }
        } finally {
            //Send build notifications if needed
            notifyBuild(currentBuild.result)
        }
    }

}