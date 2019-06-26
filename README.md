# ec2-spot-jenkins-plugin

[![Build Status](https://ci.jenkins.io/buildStatus/icon?job=Plugins/ec2-fleet-plugin/master)](https://ci.jenkins.io/blue/organizations/jenkins/Plugins%2Fec2-fleet-plugin/activity) [![](https://img.shields.io/jenkins/plugin/v/ec2-fleet.svg)](https://github.com/jenkinsci/ec2-fleet-plugin/releases)

The EC2 Spot Jenkins plugin launches EC2 Spot instances as worker nodes for Jenkins CI server, 
automatically scaling the capacity with the load. 

* [Jenkins Page](https://wiki.jenkins.io/display/JENKINS/Amazon+EC2+Fleet+Plugin)
* [Report Issue](https://github.com/jenkinsci/ec2-fleet-plugin/issues/new)
* [Overview](#overview)
* [Change Log](#change-log)
* [Usage](#usage)
  * [Setup](#setup)
  * [Scaling](#scaling)
  * [Groovy](#groovy)
* [Development](#development)

# Overview
This plugin uses Spot Fleet to launch instances instead of directly launching them by itself. 
Amazon EC2 attempts to maintain your Spot fleet's target capacity as Spot prices change to maintain
the fleet within the specified price range. For more information, see 
[How Spot Fleet Works](http://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet.html).

# Change Log

This plugin is using [SemVersion](https://semver.org/) which means that each plugin version looks like 
```
<major number = increase only if non back compatible changes>.<increase when new features>.<increase when bug fixes>
```

As result you safe to update plugin to any version until first number is the same with what you have.

https://github.com/jenkinsci/ec2-fleet-plugin/releases

# Usage

## Setup

#### 1. Get AWS Account

[AWS account](http://aws.amazon.com/ec2/)

#### 2. Create IAM User

Specify ```programmatic access``` during creation, and record credentials 
which will be used by Jenkins EC2 Fleet Plugin to connect to your Spot Fleet
 
#### 3. Configure User permissions

Add inline policy to the user to allow it use EC2 Spot Fleet 
[AWS documentation about that](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet-requests.html#spot-fleet-prerequisites)

```json
  {
      "Version": "2012-10-17",
      "Statement": [
          {
              "Effect": "Allow",
              "Action": [
                  "ec2:*"
              ],
              "Resource": "*"
          },
          {
              "Effect": "Allow",
              "Action": [
                "iam:ListRoles",
                "iam:PassRole",
                "iam:ListInstanceProfiles"
              ],
              "Resource": "*"
          }
      ]
  }
```

#### 4. Create EC2 Spot Fleet

https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/spot-fleet-requests.html#create-spot-fleet

Make sure that you specify an SSH key that will be used later by Jenkins.

#### 5. Configure Jenkins

Once the fleet is launched, you can set it up by adding a new **EC2 Fleet** cloud in the Jenkins

1. Goto ```Manage Jenkins > Plugin Manager``` 
1. Install ```EC2 Fleet Jenkins Plugin```
1. Goto ```Manage Jenkins > Configure System```
1. Click ```Add a new cloud``` and select ```Amazon SpotFleet```
1. Configure credentials and specify EC2 Spot Fleet which you want to use

## Scaling
You can specify the scaling limits in your cloud settings. By default, Jenkins will try to scale fleet up
if there are enough tasks waiting in the build queue and scale down idle nodes after a specified idleness period.

You can use the History tab in the AWS console to view the scaling history. 

## Groovy

Below Groovy script to setup EC2 Spot Fleet Plugin for Jenkins and configure it, you can
run it by [Jenkins Script Console](https://wiki.jenkins.io/display/JENKINS/Jenkins+Script+Console)

```groovy
import com.amazonaws.services.ec2.model.InstanceType
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey.DirectEntryPrivateKeySource
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl
import hudson.plugins.sshslaves.SSHConnector
import hudson.plugins.sshslaves.verifiers.NonVerifyingKeyVerificationStrategy
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.domains.Domain
import hudson.model.*
import com.amazon.jenkins.ec2fleet.EC2FleetCloud
import jenkins.model.Jenkins

// just modify this config other code just logic
config = [
    region: "us-east-1",
    fleetId: "...",
    idleMinutes: 10,
    minSize: 0,
    maxSize: 10,
    numExecutors: 1,
    awsKeyId: "...",
    secretKey: "...",
    ec2PrivateKey: '''-----BEGIN RSA PRIVATE KEY-----
...
-----END RSA PRIVATE KEY-----'''
]

// https://github.com/jenkinsci/aws-credentials-plugin/blob/aws-credentials-1.23/src/main/java/com/cloudbees/jenkins/plugins/awscredentials/AWSCredentialsImpl.java
AWSCredentialsImpl awsCredentials = new AWSCredentialsImpl(
  CredentialsScope.GLOBAL,
  "aws-credentials",
  config.awsKeyId,
  config.secretKey,
  "my aws credentials"
)
 
BasicSSHUserPrivateKey instanceCredentials = new BasicSSHUserPrivateKey(
  CredentialsScope.GLOBAL,
  "instance-ssh-key",
  "ec2-user",
  new DirectEntryPrivateKeySource(config.ec2PrivateKey),
  "", 
  "my private key to ssh ec2 for jenkins"
)
 
// find detailed information about parameters on plugin config page or
// https://github.com/jenkinsci/ec2-fleet-plugin/blob/master/src/main/java/com/amazon/jenkins/ec2fleet/EC2FleetCloud.java
EC2FleetCloud ec2FleetCloud = new EC2FleetCloud(
  "", // fleetCloudName 
  awsCredentials.id,
  "",
  config.region,
  config.fleetId,
  "ec2-fleet",  // labels
  "", // fs root
  new SSHConnector(22, 
                   instanceCredentials.id, "", "", "", "", null, 0, 0, 
                   // consult doc for line below, this one say no host verification, but you can use more strict mode
                   // https://github.com/jenkinsci/ssh-slaves-plugin/blob/master/src/main/java/hudson/plugins/sshslaves/verifiers/NonVerifyingKeyVerificationStrategy.java
                   new NonVerifyingKeyVerificationStrategy()),
  false, // if need to use privateIpUsed
  false, // if need alwaysReconnect
  config.idleMinutes, // if need to allow downscale set > 0 in min
  config.minSize, // minSize
  config.maxSize, // maxSize
  config.numExecutors, // numExecutors
  false, // addNodeOnlyIfRunning
  false, // restrictUsage allow execute only jobs with proper label
)
 
// get Jenkins instance
Jenkins jenkins = Jenkins.getInstance()
// get credentials domain
def domain = Domain.global()
// get credentials store
def store = jenkins.getExtensionList('com.cloudbees.plugins.credentials.SystemCredentialsProvider')[0].getStore()
// add credential to store
store.addCredentials(domain, awsCredentials)
store.addCredentials(domain, instanceCredentials)
// add cloud configuration to Jenkins
jenkins.clouds.add(ec2FleetCloud)
// save current Jenkins state to disk
jenkins.save()
```

# Development

Plugin usage statistic per Jenkins version [here](https://stats.jenkins.io/pluginversions/ec2-fleet.html)

## Releasing

https://jenkins.io/doc/developer/publishing/releasing/

```bash
mvn release:prepare release:perform
```

### Jenkins 2 can't connect by SSH 

https://issues.jenkins-ci.org/browse/JENKINS-53954

### Install Java 8 on EC2 instance 

```bash
sudo yum install java-1.8.0
sudo yum remove java-1.7.0-openjdk
java -version 
```