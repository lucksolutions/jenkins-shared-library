def call(body) {

    def config = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = config
    body()

    if (config.directory == null) {
        config.directory = '.'
    }

    dir("${config.directory}") {
        withCredentials([usernamePassword(credentialsId: 'aws', usernameVariable: 'AWS_ACCESS_KEY', passwordVariable: 'AWS_SECRET_KEY')]) {
            stage("Build") {

                def variables = "-var aws_access_key=${env.AWS_ACCESS_KEY} -var aws_secret_key=${env.AWS_SECRET_KEY} "
                for (int i=0; i<config.vars.size(); ++i) {
                    def var = config.vars[i]
                    variables = variables + "-var ${var} "
                }

                sh "packer build ${variables} ${config.packerFile}"
            }
        }
    }
}