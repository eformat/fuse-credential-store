node('rhel') {
    stage 'Checkout'
    checkout scm

    stage 'Build'
    withEnv(["JAVA_HOME=${ tool 'openjdk8' }"]) {
        sh './mvnw -q -B -V -DskipTests'
    }

    stage 'Test'
    withEnv(["JAVA_HOME=${ tool 'openjdk8' }"]) {
        sh './mvnw -q -B -V test -Dmaven.test.failure.ignore'
    }
    step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
}
