# Configuration As Code

[Jenkins Configuration As Code](https://jenkins.io/projects/jcasc/)

## Properties

### EC2FleetCloud

[Definition](https://github.com/jenkinsci/ec2-fleet-plugin/blob/master/src/main/java/com/amazon/jenkins/ec2fleet/EC2FleetCloud.java#L156-L179)

| Property        | Type           | Required  | Description |
|-------------------|---|---|---|
| name              |string|yes|ec2-fleet|
| awsCredentialsId  |string|no, default ```null```|[Leave blank to use AWS EC2 instance role](https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_use_switch-role-ec2.html)|
| computerConnector |object|yes|for example ```sshConnector```|
| region            |string|yes|```us-east-2```, full [list](https://docs.aws.amazon.com/AmazonRDS/latest/UserGuide/Concepts.RegionsAndAvailabilityZones.html)|
| fleet   |string|yes|my-fleet|
| endpoint|string|no|Set only if you need to use custome endpoint ```http://a.com```|
| fsRoot  |string|no|my-root|
| privateIpUsed|boolean|no, default ```false```|connect to EC2 instance by private id instead of public|
| alwaysReconnect|boolean|no, default ```false```||
| labelString |string|yes||
| idleMinutes |int|no, default ```0```||
| minSize     |int|no, default ```0```||
| maxSize     |int|no, default ```0```||
| numExecutors|int|no, default ```1```||
| addNodeOnlyIfRunning|boolean|no, default ```false```||
| restrictUsage|boolean|no, default ```false```|if ```true``` fleet nodes will executed only jobs with same label|
| scaleExecutorsByWeight|boolean|no, default ```false```||
| initOnlineTimeoutSec|int|no, default ```180```||
| initOnlineCheckIntervalSec|int|no, default ```15```||
| cloudStatusIntervalSec|int|no, default ```10```||
| disableTaskResubmit|boolean|no, default ```false```||
| noDelayProvision|boolean|no, default ```false```||

## EC2FleetLabelCloud

More about this type [here](LABEL-BASED-CONFIGURATION.md)

[Definition](https://github.com/jenkinsci/ec2-fleet-plugin/blob/master/src/main/java/com/amazon/jenkins/ec2fleet/EC2FleetLabelCloud.java#L123-L145)

## Examples

### EC2FleetCloud (min set of properties)

```yaml
jenkins:
  clouds:
    - ec2Fleet:
        name: ec2-fleet
        computerConnector:
            sshConnector:
                credentialsId: cred
                sshHostKeyVerificationStrategy:
                  NonVerifyingKeyVerificationStrategy
        region: us-east-2
        fleet: my-fleet
        minSize: 1
        maxSize: 10
```

### EC2FleetCloud (All properties)

```yaml
jenkins:
  clouds:
    - ec2Fleet:
        name: ec2-fleet
        awsCredentialsId: xx
        computerConnector:
            sshConnector:
                credentialsId: cred
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
