//Skip the build if this was caused by branch indexing and its not the first build
def call() {

    //Check to see if its the first build
    if (currentBuild.getPreviousBuild() != null) {
        
        def causes = currentBuild.rawBuild.getCauses()
        for (int i=0; i<causes.size(); ++i) {
            if (causes[i].getShortDescription().contains('Branch indexing')) {
                //Don't build since this was just branch indexing
                currentBuild.result = 'SUCCESSFUL'
                echo('Stopping since we dont want to build on every index...')
                return
            }
        }
    }
}