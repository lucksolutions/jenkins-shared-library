def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    dir("${config.directory}") {
        withCredentials([amazonWebServicesCredentials(credentialsId: 'aws', accessKeyVariable: 'AWS_ACCESS_KEY', secretKeyVariable: 'AWS_SECRET_KEY')]) {
            stage("Build") {

                def variables = "-var aws_access_key=${AWS_ACCESS_KEY} -var aws_secret_key=${AWS_SECRET_KEY} "
                for (int i=0; i<config.vars.size(); ++i) {
                    def var = config.vars[i]
                    variables = variables + "-var ${var} "
                }

                sh "packer build ${variables} ${config.packerFile}"
            }
        }
    }
}