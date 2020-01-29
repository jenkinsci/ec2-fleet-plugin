[Jenkins Configuration As Code](https://jenkins.io/projects/jcasc/)

## EC2FleetCloud Configuration (min set of properties)

```yaml
jenkins:
  clouds:
    - ec2Fleet:
        name: ec2-fleet
        awsCredentialsId: xx
        computerConnector:
            sshConnector:
                credentialsId: cred
                port: 22
        region: us-east-2
        fleet: my-fleet
        minSize: 15
        maxSize: 90
```

## EC2FleetCloud Configuration (All properties)

```yaml
jenkins:
  clouds:
    - ec2Fleet:
        name: ec2-fleet
        awsCredentialsId: xx
        computerConnector:
            sshConnector:
                credentialsId: cred
                port: 22
        region: us-east-2
        endpoint: http://a.com
        fleet: my-fleet
        fsRoot: my-root
        privateIpUsed: true
        alwaysReconnect: true
        labelString: myLabel
        idleMinutes: 33
        minSize: 15
        maxSize: 90
        numExecutors: 12
        addNodeOnlyIfRunning: true
        restrictUsage: true
        scaleExecutorsByWeight: true
        initOnlineTimeoutSec: 181
        initOnlineCheckIntervalSec: 13
        cloudStatusIntervalSec: 11
        disableTaskResubmit: true
        noDelayProvision: true
```